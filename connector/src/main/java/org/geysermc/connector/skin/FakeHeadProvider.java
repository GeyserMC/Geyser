/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.skin;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerSkinPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Jan / qlow (11.11.21)
 */
public class FakeHeadProvider {

    private static final Cache<String, SkinProvider.Skin> cachedMergedSkins = CacheBuilder.newBuilder()
            .expireAfterAccess( 1, TimeUnit.HOURS )
            .maximumSize( 10000 )
            .build();

    public static void setHead( GeyserSession session, PlayerEntity entity, CompoundTag profileTag ) {
        /*if ( session.getFakeHeadCache().getPlayersWithCustomSkin().contains( entity.getUuid() ) ) {
            // TODO REMOVE THIS IF, AS IT DOESN'T ALLOW CHANGING HEADS
            return;
        }*/

        session.getFakeHeadCache().addCustomSkin( entity.getUuid() );

        GameProfile gameProfile = new GameProfile( UUID.randomUUID(), ( ( StringTag ) profileTag.get( "Name" ) ).getValue() );

        if ( profileTag.contains( "Properties" ) ) {
            List<GameProfile.Property> properties = new ArrayList<>();
            CompoundTag propertiesTag = profileTag.get( "Properties" );

            for ( String key : propertiesTag.keySet() ) {
                ListTag propertyArrayTag = propertiesTag.get( key );

                for ( Tag tag : propertyArrayTag ) {
                    if ( tag instanceof CompoundTag ) {
                        CompoundTag propertyTag = ( CompoundTag ) tag;

                        if ( propertyTag.contains( "Signature" ) ) {
                            properties.add( new GameProfile.Property( key,
                                    ( ( StringTag ) propertyTag.get( "Value" ) ).getValue(),
                                    ( ( StringTag ) propertyTag.get( "Signature" ) ).getValue() ) );
                        } else {
                            properties.add( new GameProfile.Property( key, ( ( StringTag ) propertyTag.get( "Value" ) ).getValue() ) );
                        }
                    }
                }
            }

            gameProfile.setProperties( properties );
        }

        SkinManager.GameProfileData data = SkinManager.GameProfileData.from( entity.getProfile() );
        String skinUrl = SkinManager.GameProfileData.from( gameProfile ).getSkinUrl();

        SkinProvider.requestSkinAndCape( entity.getUuid(), data.getSkinUrl(), data.getCapeUrl() )
                .whenCompleteAsync( ( skinAndCape, throwable ) -> {
                    try {
                        SkinProvider.Skin skin = skinAndCape.getSkin();
                        SkinProvider.Cape cape = skinAndCape.getCape();
                        SkinProvider.SkinGeometry geometry = SkinProvider.SkinGeometry.getLegacy( data.isAlex() );

                        if ( cape.isFailed() ) {
                            cape = SkinProvider.getOrDefault( SkinProvider.requestBedrockCape( entity.getUuid() ),
                                    SkinProvider.EMPTY_CAPE, 3 );
                        }

                        if ( cape.isFailed() && SkinProvider.ALLOW_THIRD_PARTY_CAPES ) {
                            cape = SkinProvider.getOrDefault( SkinProvider.requestUnofficialCape(
                                    cape, entity.getUuid(),
                                    entity.getUsername(), false
                            ), SkinProvider.EMPTY_CAPE, SkinProvider.CapeProvider.VALUES.length * 3 );
                        }

                        geometry = SkinProvider.getOrDefault( SkinProvider.requestBedrockGeometry(
                                geometry, entity.getUuid()
                        ), geometry, 3 );

                        if ( session.getUpstream().isInitialized() ) {
                            String skinKey = skinUrl + "_" + entity.getUuid();
                            SkinProvider.Skin targetSkin = null;

                            if ( ( targetSkin = cachedMergedSkins.getIfPresent( skinKey ) ) == null ) {
                                SkinProvider.Skin otherSkin = SkinProvider.getOrDefault(
                                        SkinProvider.requestSkin( entity.getUuid(), skinUrl, false ), SkinProvider.EMPTY_SKIN, 5 );
                                BufferedImage originalSkinImage = SkinProvider.imageDataToBufferedImage( skin.getSkinData(), 64, skin.getSkinData().length / 4 / 64 ); //ImageIO.read( originalSkinInput );
                                BufferedImage otherSkinImage = SkinProvider.imageDataToBufferedImage( otherSkin.getSkinData(), 64, otherSkin.getSkinData().length / 4 / 64 );//ImageIO.read( otherSkinInput );

                                Graphics2D graphics2D = originalSkinImage.createGraphics();
                                graphics2D.setColor( Color.WHITE );
                                graphics2D.drawImage( otherSkinImage, 0, 0, 16, 64, null );
                                //graphics2D.fill( new Rectangle(0,0,64,16) );
                                //graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                                //graphics2D.clearRect( 0, 0, 64, 16 );
                                //graphics2D.drawImage( otherSkinImage, 0, 0, 64, 16, null );
                                graphics2D.dispose();

                                ImageIO.write( originalSkinImage, "png", new File( "cache/" + new String( Base64.getEncoder().encode( skinKey.getBytes( StandardCharsets.UTF_8 ) ) ) + ".png" ) );

                                byte[] targetSkinData = SkinProvider.bufferedImageToImageData( originalSkinImage );
                                targetSkin = new SkinProvider.Skin( entity.getUuid(), skinKey, targetSkinData, System.currentTimeMillis(), false, false );
                                cachedMergedSkins.put( skinKey, targetSkin );
                            }

                            PlayerSkinPacket packet = new PlayerSkinPacket();
                            packet.setUuid( entity.getUuid() );
                            packet.setOldSkinName( "" );
                            packet.setNewSkinName( skinKey );
                            packet.setSkin( getSkin( skinKey, targetSkin, cape, geometry ) );
                            packet.setTrustedSkin( true );
                            session.sendUpstreamPacket( packet );
                        }
                    } catch ( Exception e ) {
                        GeyserConnector.getInstance().getLogger().error( LanguageUtils.getLocaleStringLog( "geyser.skin.fail", entity.getUuid() ), e );
                    }
                } );

    }

    public static void restoreOriginalSkin( GeyserSession session, LivingEntity livingEntity ) {
        if ( !( livingEntity instanceof PlayerEntity ) ) {
            return;
        }

        PlayerEntity entity = ( PlayerEntity ) livingEntity;

        if ( !session.getFakeHeadCache().getPlayersWithCustomSkin().contains( entity.getUuid() ) ) {
            return;
        }

        SkinManager.GameProfileData data = SkinManager.GameProfileData.from( entity.getProfile() );

        // TODO RESET SKIN
        SkinProvider.requestSkinAndCape( entity.getUuid(), data.getSkinUrl(), data.getCapeUrl() )
                .whenCompleteAsync( ( skinAndCape, throwable ) -> {
                    try {
                        SkinProvider.Skin skin = skinAndCape.getSkin();
                        SkinProvider.Cape cape = skinAndCape.getCape();
                        SkinProvider.SkinGeometry geometry = SkinProvider.SkinGeometry.getLegacy( data.isAlex() );

                        if ( cape.isFailed() ) {
                            cape = SkinProvider.getOrDefault( SkinProvider.requestBedrockCape( entity.getUuid() ),
                                    SkinProvider.EMPTY_CAPE, 3 );
                        }

                        if ( cape.isFailed() && SkinProvider.ALLOW_THIRD_PARTY_CAPES ) {
                            cape = SkinProvider.getOrDefault( SkinProvider.requestUnofficialCape(
                                    cape, entity.getUuid(),
                                    entity.getUsername(), false
                            ), SkinProvider.EMPTY_CAPE, SkinProvider.CapeProvider.VALUES.length * 3 );
                        }

                        geometry = SkinProvider.getOrDefault( SkinProvider.requestBedrockGeometry(
                                geometry, entity.getUuid()
                        ), geometry, 3 );

                        if ( session.getUpstream().isInitialized() ) {
                            PlayerSkinPacket packet = new PlayerSkinPacket();
                            packet.setUuid( entity.getUuid() );
                            packet.setOldSkinName( "" );
                            packet.setNewSkinName( skin.getTextureUrl() );
                            packet.setSkin( getSkin( skin.getTextureUrl(), skin, cape, geometry ) );
                            packet.setTrustedSkin( true );
                            session.sendUpstreamPacket( packet );
                        }
                    } catch ( Exception e ) {
                        GeyserConnector.getInstance().getLogger().error( LanguageUtils.getLocaleStringLog( "geyser.skin.fail", entity.getUuid() ), e );
                    }
                } );

        session.getFakeHeadCache().removeEntity( entity );
    }

    private static SerializedSkin getSkin( String skinId, SkinProvider.Skin skin, SkinProvider.Cape cape, SkinProvider.SkinGeometry geometry ) {
        return SerializedSkin.of( skinId, "", geometry.getGeometryName(),
                ImageData.of( skin.getSkinData() ), Collections.emptyList(),
                ImageData.of( cape.getCapeData() ), geometry.getGeometryData(),
                "", true, false, false, cape.getCapeId(), skinId );
    }


}
