package rocket.giovanniclient.giovanniclient.repo;

import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.pack.ModPackResourcesSorter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FileUtil;
import rocket.giovanniclient.giovanniclient.GiovanniClient;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiItemRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public final class GiovanniRepoResourcePack implements ModResourcePack {
    private static final String NAMESPACE = "neurepo";
    private final Path basePath;

    public GiovanniRepoResourcePack(Path basePath) {
        this.basePath = basePath;
    }

    public static void append(ModPackResourcesSorter sorter) {
        sorter.addPack(new GiovanniRepoResourcePack(SkyBlockReiItemRepository.getRepoDirectoryForResources()));
    }

    public static Optional<Resource> createResourceDirectly(Identifier identifier) {
        GiovanniRepoResourcePack pack = new GiovanniRepoResourcePack(SkyBlockReiItemRepository.getRepoDirectoryForResources());
        IoSupplier<InputStream> resource = pack.getResource(PackType.CLIENT_RESOURCES, identifier);
        if (resource == null) {
            return Optional.empty();
        }

        return Optional.of(new Resource(pack, resource, () -> ResourceMetadata.EMPTY));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        Path file = getFile(segments);
        return file == null ? null : IoSupplier.create(file);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(id.getNamespace())) {
            return null;
        }

        Path file = getFile(id.getPath().split("/"));
        return file == null ? null : IoSupplier.create(file);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, PackResources.ResourceOutput consumer) {
        if (type != PackType.CLIENT_RESOURCES || !NAMESPACE.equals(namespace)) {
            return;
        }

        Path prefixPath = basePath.resolve(prefix).normalize();
        if (!prefixPath.startsWith(basePath) || !Files.isDirectory(prefixPath)) {
            return;
        }

        try (var paths = Files.walk(prefixPath)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Path relative = basePath.relativize(path);
                Identifier id = Identifier.tryBuild(NAMESPACE, relative.toString().replace('\\', '/'));
                if (id != null) {
                    consumer.accept(id, IoSupplier.create(path));
                }
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(NAMESPACE) : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> metadataSectionType) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return new PackLocationInfo(NAMESPACE, Component.literal("GiovanniClient NEU Repo"), PackSource.BUILT_IN, Optional.empty());
    }

    @Override
    public ModMetadata getFabricModMetadata() {
        return FabricLoader.getInstance().getModContainer(GiovanniClient.MOD_ID).orElseThrow().getMetadata();
    }

    @Override
    public ModResourcePack createOverlay(String overlay) {
        return new GiovanniRepoResourcePack(basePath.resolve(overlay));
    }

    @Override
    public void close() {
    }

    private Path getFile(String[] segments) {
        FileUtil.validatePath(segments);
        Path path = basePath;
        for (String segment : segments) {
            path = path.resolve(segment);
        }

        Path normalized = path.normalize();
        return normalized.startsWith(basePath) && Files.isRegularFile(normalized) ? normalized : null;
    }
}
