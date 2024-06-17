package me.noahvdaa.healthhider;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static net.minecraft.world.entity.LivingEntity.DATA_HEALTH_ID;

public class HHHandler extends MessageToMessageEncoder<Packet<?>> {
    private final HealthHider plugin;

    public HHHandler(HealthHider plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) {
        return msg instanceof ClientboundSetEntityDataPacket || msg instanceof ClientboundBundlePacket;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> msg, List<Object> out) {
        Connection connection = (Connection) ctx.pipeline().get("packet_handler");
        ServerPlayer player = connection.getPlayer();

        if (msg instanceof ClientboundSetEntityDataPacket entityDataPacket) {
            out.add(obfuscate(player, entityDataPacket));
        } else if (msg instanceof ClientboundBundlePacket bundlePacket) {
            out.add(new ClientboundBundlePacket(() -> StreamSupport.stream(bundlePacket.subPackets().spliterator(), false).<Packet<? super ClientGamePacketListener>> map(subPacket -> {
                if (subPacket instanceof ClientboundSetEntityDataPacket entityDataPacket) {
                    return obfuscate(player, entityDataPacket);
                } else {
                    return subPacket;
                }
            }).iterator()));
        }
    }

    private ClientboundSetEntityDataPacket obfuscate(ServerPlayer player, ClientboundSetEntityDataPacket entityDataPacket) {
        if (!shouldObfuscate(player, entityDataPacket)) {
            return entityDataPacket;
        }

        List<SynchedEntityData.DataValue<?>> packed = new ArrayList<>(entityDataPacket.packedItems());
        packed.replaceAll(dataValue -> {
            if (dataValue.id() == DATA_HEALTH_ID.id()) {
                float health = (float) dataValue.value();
                float shownHealth;
                if (health <= 0.0F) {
                    shownHealth = health;
                } else {
                    shownHealth = 1F;
                }
                return new SynchedEntityData.DataValue<>(dataValue.id(), EntityDataSerializers.FLOAT, shownHealth);
            } else {
                return dataValue;
            }
        });

        return new ClientboundSetEntityDataPacket(entityDataPacket.id(), packed);
    }

    private boolean shouldObfuscate(ServerPlayer player, ClientboundSetEntityDataPacket entityDataPacket) {
        if (player == null || entityDataPacket.id() == player.getId()) {
            // We don't want to hide our own health
            return false;
        }

        Entity entity = player.serverLevel().moonrise$getEntityLookup().get(entityDataPacket.id());

        if (!(entity instanceof LivingEntity)) {
            // Only living entities have health
            return false;
        }

        HHConfig config = plugin.configuration();
        if (config.entities().contains(entity.getType()) != config.whitelistMode()) {
            // We don't need to censor this entity
            return false;
        }

        if (config.enableBypassPermission() && player.getBukkitEntity().hasPermission("healthider.bypass")) {
            return false;
        }

        return true;
    }
}
