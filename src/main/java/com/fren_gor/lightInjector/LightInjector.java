// MIT License
//
// Copyright (c) 2022 fren_gor
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.fren_gor.lightInjector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * A light yet complete and fast packet injector for Spigot servers.
 * <p>
 * Can listen to every packet since {@link AsyncPlayerPreLoginEvent} fires.
 * <p>
 * Local compatibility note: newer Authlib versions expose GameProfile UUIDs
 * through a record-style {@code id()} accessor instead of {@code getId()}.
 * This copy resolves that method reflectively to avoid login-time
 * NoSuchMethodError on modern Paper/Purpur builds.
 *
 * @author fren_gor
 */
public abstract class LightInjector {

    private static final int VERSION = Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);
    private static final String COMPLETE_VERSION = VERSION >= 17 ? null : Bukkit.getServer().getClass().getName().split("\\.")[3];
    private static final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();

    private static final Class<?> SERVER_CLASS = getNMSClass("MinecraftServer", "MinecraftServer", "server");
    private static final Class<?> SERVER_CONNECTION_CLASS = getNMSClass("ServerConnection", "ServerConnectionListener", "server.network");
    private static final Class<?> NETWORK_MANAGER_CLASS = getNMSClass("NetworkManager", "Connection", "network");
    private static final Class<?> ENTITY_PLAYER_CLASS = getNMSClass("EntityPlayer", "ServerPlayer", "server.level");
    private static final Class<?> PLAYER_CONNECTION_CLASS = getNMSClass("PlayerConnection", "ServerGamePacketListenerImpl", "server.network");
    private static final Class<?> PACKET_LOGIN_OUT_SUCCESS_CLASS = getNMSClass("PacketLoginOutSuccess", "ClientboundGameProfilePacket", "network.protocol.login");
    private static final Class<?> GAME_PROFILE_CLASS = getClass("com.mojang.authlib.GameProfile");

    private static final Field NMS_SERVER = getField(getCBClass("CraftServer"), SERVER_CLASS, 1);
    private static final Field NMS_SERVER_CONNECTION = getField(SERVER_CLASS, SERVER_CONNECTION_CLASS, 1);
    private static final Field NMS_NETWORK_MANAGERS_LIST = getField(SERVER_CONNECTION_CLASS, List.class, 2);

    @Nullable
    private static final Field NMS_PENDING_NETWORK_MANAGERS = getPendingNetworkManagersFieldOrNull(SERVER_CONNECTION_CLASS);

    private static final Field NMS_CHANNEL_FROM_NM = getField(NETWORK_MANAGER_CLASS, Channel.class, 1);
    private static final Field GAME_PROFILE_FROM_PACKET = getField(PACKET_LOGIN_OUT_SUCCESS_CLASS, GAME_PROFILE_CLASS, 1);
    private static final Field GET_PLAYER_CONNECTION = getField(ENTITY_PLAYER_CLASS, PLAYER_CONNECTION_CLASS, 1);
    private static final Field GET_NETWORK_MANAGER = getField(PLAYER_CONNECTION_CLASS, NETWORK_MANAGER_CLASS, 1, 1);

    private static final Method GET_PLAYER_HANDLE = getMethod(getCBClass("entity.CraftPlayer"), "getHandle");

    private static int ID = 0;

    private final Plugin plugin;
    private final String identifier;
    private final List<?> networkManagers;

    @Nullable
    private final Iterable<?> pendingNetworkManagers;

    private final EventListener listener = new EventListener();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<UUID, Player> playerCache = Collections.synchronizedMap(new HashMap<>());
    private final Set<Channel> injectedChannels = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public LightInjector(@NotNull Plugin plugin) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("LightInjector must be constructed on the main thread.");
        }
        if (!Objects.requireNonNull(plugin, "Plugin is null.").isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getName() + " is not enabled");
        }

        this.plugin = plugin;
        this.identifier = Objects.requireNonNull(getIdentifier(), "getIdentifier() returned a null value.") + '-' + ID++;

        try {
            Object conn = NMS_SERVER_CONNECTION.get(NMS_SERVER.get(Bukkit.getServer()));
            if (conn == null) {
                throw new RuntimeException("[LightInjector] ServerConnection is null.");
            }

            networkManagers = (List<?>) NMS_NETWORK_MANAGERS_LIST.get(conn);
            pendingNetworkManagers = NMS_PENDING_NETWORK_MANAGERS != null
                    ? (Iterable<?>) NMS_PENDING_NETWORK_MANAGERS.get(conn)
                    : null;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] An error occurred while injecting.", exception);
        }

        Bukkit.getPluginManager().registerEvents(listener, plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                injectPlayer(player);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "[LightInjector] An error occurred while injecting a player:", exception);
            }
        }
    }

    protected abstract @Nullable Object onPacketReceiveAsync(@Nullable Player sender, @NotNull Channel channel, @NotNull Object packet);

    protected abstract @Nullable Object onPacketSendAsync(@Nullable Player receiver, @NotNull Channel channel, @NotNull Object packet);

    public final void sendPacket(@NotNull Player receiver, @NotNull Object packet) {
        Objects.requireNonNull(receiver, "Player is null.");
        Objects.requireNonNull(packet, "Packet is null.");
        sendPacket(getChannel(receiver), packet);
    }

    public final void sendPacket(@NotNull Channel channel, @NotNull Object packet) {
        Objects.requireNonNull(channel, "Channel is null.");
        Objects.requireNonNull(packet, "Packet is null.");
        channel.pipeline().writeAndFlush(packet);
    }

    public final void receivePacket(@NotNull Player sender, @NotNull Object packet) {
        Objects.requireNonNull(sender, "Player is null.");
        Objects.requireNonNull(packet, "Packet is null.");
        receivePacket(getChannel(sender), packet);
    }

    public final void receivePacket(@NotNull Channel channel, @NotNull Object packet) {
        Objects.requireNonNull(channel, "Channel is null.");
        Objects.requireNonNull(packet, "Packet is null.");
        ChannelHandlerContext encoder = channel.pipeline().context("encoder");
        Objects.requireNonNull(encoder, "Channel is not a player channel").fireChannelRead(packet);
    }

    protected @NotNull String getIdentifier() {
        return "light-injector-" + plugin.getName();
    }

    public final void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        listener.unregister();

        synchronized (networkManagers) {
            for (Object manager : networkManagers) {
                try {
                    Channel channel = getChannel(manager);
                    channel.eventLoop().submit(() -> channel.pipeline().remove(identifier));
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.SEVERE, "[LightInjector] An error occurred while uninjecting a player:", exception);
                }
            }
        }

        playerCache.clear();
        injectedChannels.clear();
    }

    public final boolean isClosed() {
        return closed.get();
    }

    public final @NotNull Plugin getPlugin() {
        return plugin;
    }

    private void injectPlayer(Player player) {
        injectChannel(getChannel(player)).player = player;
    }

    private PacketHandler injectChannel(Channel channel) {
        PacketHandler handler = new PacketHandler();

        channel.eventLoop().submit(() -> {
            if (isClosed()) return;

            if (injectedChannels.add(channel)) {
                try {
                    channel.pipeline().addBefore("packet_handler", identifier, handler);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().severe("[LightInjector] Could not inject a player, a handler with identifier '" + identifier + "' is already present");
                }
            }
        });

        return handler;
    }

    private void injectNetworkManager(Object networkManager) {
        Channel channel = getChannel(networkManager);
        if (!injectedChannels.contains(channel)) {
            injectChannel(channel);
        }
    }

    private Object getNetworkManager(Player player) {
        try {
            return GET_NETWORK_MANAGER.get(GET_PLAYER_CONNECTION.get(GET_PLAYER_HANDLE.invoke(player)));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] Could not get player's network manager.", exception);
        }
    }

    private Channel getChannel(Player player) {
        return getChannel(getNetworkManager(player));
    }

    private Channel getChannel(Object networkManager) {
        try {
            return (Channel) NMS_CHANNEL_FROM_NM.get(networkManager);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] Could not get network manager's channel.", exception);
        }
    }

    private final class EventListener implements Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        private void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
            if (isClosed()) {
                return;
            }

            synchronized (networkManagers) {
                if (networkManagers instanceof RandomAccess) {
                    for (int i = networkManagers.size() - 1; i >= 0; i--) {
                        injectNetworkManager(networkManagers.get(i));
                    }
                } else {
                    for (Object networkManager : networkManagers) {
                        injectNetworkManager(networkManager);
                    }
                }

                if (pendingNetworkManagers != null) {
                    synchronized (pendingNetworkManagers) {
                        for (Object networkManager : pendingNetworkManagers) {
                            injectNetworkManager(networkManager);
                        }
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        private void onPlayerLoginEvent(PlayerLoginEvent event) {
            if (isClosed()) {
                return;
            }

            playerCache.put(event.getPlayer().getUniqueId(), event.getPlayer());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        private void onPlayerJoinEvent(PlayerJoinEvent event) {
            if (isClosed()) {
                return;
            }
            Player player = event.getPlayer();

            Object networkManager = getNetworkManager(player);
            Channel channel = getChannel(networkManager);
            @Nullable ChannelHandler channelHandler = channel.pipeline().get(identifier);
            if (channelHandler != null) {
                if (channelHandler instanceof PacketHandler) {
                    ((PacketHandler) channelHandler).player = player;
                    playerCache.remove(player.getUniqueId());
                }
                return;
            }

            plugin.getLogger().info("[LightInjector] Late injection for player " + player.getName());
            injectChannel(channel).player = player;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onPluginDisableEvent(PluginDisableEvent event) {
            if (plugin.equals(event.getPlugin())) {
                close();
            }
        }

        private void unregister() {
            AsyncPlayerPreLoginEvent.getHandlerList().unregister(this);
            PlayerLoginEvent.getHandlerList().unregister(this);
            PlayerJoinEvent.getHandlerList().unregister(this);
            PluginDisableEvent.getHandlerList().unregister(this);
        }
    }

    private final class PacketHandler extends ChannelDuplexHandler {
        private volatile Player player;

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            injectedChannels.remove(ctx.channel());
            super.channelUnregistered(ctx);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
            if (player == null && PACKET_LOGIN_OUT_SUCCESS_CLASS.isInstance(packet)) {
                try {
                    Object profile = GAME_PROFILE_FROM_PACKET.get(packet);
                    UUID profileId = getGameProfileId(profile);
                    if (profileId != null) {
                        @Nullable Player cachedPlayer = playerCache.remove(profileId);
                        if (cachedPlayer != null) {
                            this.player = cachedPlayer;
                        }
                    }
                } catch (ReflectiveOperationException exception) {
                    plugin.getLogger().log(Level.SEVERE, "[LightInjector] An error occurred while handling PacketLoginOutSuccess:", exception);
                }
            }

            @Nullable Object newPacket;
            try {
                newPacket = onPacketSendAsync(player, ctx.channel(), packet);
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "[LightInjector] An error occurred while calling onPacketSendAsync:", throwable);
                super.write(ctx, packet, promise);
                return;
            }
            if (newPacket != null)
                super.write(ctx, newPacket, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
            @Nullable Object newPacket;
            try {
                newPacket = onPacketReceiveAsync(player, ctx.channel(), packet);
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "[LightInjector] An error occurred while calling onPacketReceiveAsync:", throwable);
                super.channelRead(ctx, packet);
                return;
            }
            if (newPacket != null)
                super.channelRead(ctx, newPacket);
        }
    }

    private static @Nullable UUID getGameProfileId(Object profile) throws ReflectiveOperationException {
        if (profile == null)
            return null;

        UUID uuid = asUuid(invokeNoArg(profile, "getId"));
        return uuid != null ? uuid : asUuid(invokeNoArg(profile, "id"));
    }

    private static @Nullable UUID asUuid(@Nullable Object value) {
        if (value instanceof UUID)
            return (UUID) value;
        if (value instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) value;
            if (optional.isPresent() && optional.get() instanceof UUID)
                return (UUID) optional.get();
        }
        return null;
    }

    private static @Nullable Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Class<?> getNMSClass(String spigotMappedName, String mojangMappedName, String mcPackage) {
        String path = "net.minecraft." + (VERSION >= 17 ? mcPackage : "server." + COMPLETE_VERSION) + '.';
        try {
            return Class.forName(path + spigotMappedName);
        } catch (ClassNotFoundException exception) {
            try {
                return Class.forName(path + mojangMappedName);
            } catch (ClassNotFoundException ignored) {
                throw new RuntimeException("[LightInjector] Can not find NMS Class! (" + path + spigotMappedName + ')', exception);
            }
        }
    }

    private static Class<?> getClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("[LightInjector] Can not find Class! (" + name + ')', exception);
        }
    }

    private static Class<?> getCBClass(String name) {
        String clazz = CRAFTBUKKIT_PACKAGE + "." + name;
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("[LightInjector] Can not find CB Class! (" + clazz + ')', exception);
        }
    }

    private static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("[LightInjector] Can not find field! (" + clazz.getName() + '.' + name + ')', exception);
        }
    }

    private static Field getField(Class<?> clazz, Class<?> type, @Range(from = 1, to = Integer.MAX_VALUE) int index) {
        return getField(clazz, type, index, 0);
    }

    private static Field getField(Class<?> clazz, Class<?> type, @Range(from = 1, to = Integer.MAX_VALUE) int index,
                                  @Range(from = 0, to = Integer.MAX_VALUE) int superClassesToTry) {
        final Class<?> savedClazz = clazz;
        final int savedIndex = index;

        for (int i = 0; i <= superClassesToTry; i++) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (type.equals(field.getType()) && --index <= 0) {
                    field.setAccessible(true);
                    return field;
                }
            }
            index = savedIndex;
            for (Field field : fields) {
                if (type.isAssignableFrom(field.getType()) && --index <= 0) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz == null || clazz == Object.class) {
                break;
            }
            index = savedIndex;
        }

        String errorMsg = "[LightInjector] Can not find field! (" + savedIndex + getOrdinal(savedIndex) + type.getName() + " in " + savedClazz.getName();
        if (superClassesToTry > 0) {
            errorMsg += " and in its " + superClassesToTry + (superClassesToTry == 1 ? " super class" : " super classes");
        }
        errorMsg += ')';

        throw new RuntimeException(errorMsg);
    }

    @Nullable
    private static Field getPendingNetworkManagersFieldOrNull(Class<?> serverConnectionClass) {
        try {
            Field pending = getField(serverConnectionClass, "pending");
            if (pending.getType() == Queue.class || pending.getType() == List.class) {
                return pending;
            }
        } catch (Exception ignored) {
        }

        try {
            return getField(serverConnectionClass, Queue.class, 1);
        } catch (Exception ignored) {
        }
        try {
            return getField(serverConnectionClass, List.class, 3);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException exception) {
            StringJoiner params = new StringJoiner(", ");
            for (Class<?> parameter : parameters) {
                params.add(parameter.getName());
            }
            throw new RuntimeException("[LightInjector] Can not find method! (" + clazz.getName() + '.' + name + '(' + params + ')', exception);
        }
    }

    private static String getOrdinal(int number) {
        switch (number) {
            case 1:
                return "st ";
            case 2:
                return "nd ";
            case 3:
                return "rd ";
            default:
                return "th ";
        }
    }
}
