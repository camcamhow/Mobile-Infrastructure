package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialFeedPacket(List<PostEntry> posts) implements CustomPacketPayload {
    public static final Type<SocialFeedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_feed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialFeedPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(Math.min(100, packet.posts().size()));
                packet.posts().stream().limit(100).forEach(post -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, post.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, post.authorId());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, post.author());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, post.text());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, post.attachment());
                    buffer.writeBoolean(post.hasImage());
                    buffer.writeLong(post.createdAt());
                    buffer.writeVarInt(post.likes());
                    buffer.writeBoolean(post.liked());
                    buffer.writeVarInt(Math.min(20, post.comments().size()));
                    post.comments().stream().limit(20).forEach(comment ->
                            ByteBufCodecs.STRING_UTF8.encode(buffer, comment));
                });
            }, buffer -> {
                int count = Math.clamp(buffer.readVarInt(), 0, 100);
                List<PostEntry> posts = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String authorId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String author = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String text = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String attachment = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    boolean hasImage = buffer.readBoolean();
                    long createdAt = buffer.readLong();
                    int likes = Math.max(0, buffer.readVarInt());
                    boolean liked = buffer.readBoolean();
                    int commentCount = Math.clamp(buffer.readVarInt(), 0, 20);
                    List<String> comments = new ArrayList<>(commentCount);
                    for (int c = 0; c < commentCount; c++) {
                        comments.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                    }
                    posts.add(new PostEntry(id, authorId, author, text, attachment, hasImage,
                            createdAt, likes, liked, comments));
                }
                return new SocialFeedPacket(posts);
            });

    public record PostEntry(String id, String authorId, String author, String text, String attachment,
                            boolean hasImage, long createdAt, int likes, boolean liked, List<String> comments) {
        public PostEntry {
            comments = List.copyOf(comments);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
