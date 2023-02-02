package i18nupdatemod.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import i18nupdatemod.util.Log;
import i18nupdatemod.util.Version;
import i18nupdatemod.util.VersionRange;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AssetConfig {
    /**
     * <a href="https://github.com/CFPAOrg/Minecraft-Mod-Language-Package">CFPAOrg/Minecraft-Mod-Language-Package</a>
     */
    private static final String CFPA_ASSET_ROOT = "https://gitcode.net/chearlai/translationpackmirror/-/raw/main/files-2444-T/";
    private static final Gson GSON = new Gson();
    private static final java.lang.reflect.Type ASSET_INDEX_LIST_TYPE = new TypeToken<List<AssetIndex>>() {
    }.getType();
    private static List<AssetIndex> i18NAssetIndex;

    static {
        init();
    }

    private static void init() {
        try (InputStream is = AssetConfig.class.getResourceAsStream("/i18nAssetIndex.json")) {
            if (is != null) {
                i18NAssetIndex = GSON.fromJson(new InputStreamReader(is), ASSET_INDEX_LIST_TYPE);
            } else {
                Log.warning("Error getting index: is is null");
            }
        } catch (Exception e) {
            Log.warning("Error getting index: " + e);
        }
    }

    public static class AssetInfo {
        public List<AssetDownloadInfo> downloads;
        @Nullable
        public Integer covertPackFormat;
        @Nullable
        public String covertFileName;

        public static class AssetDownloadInfo {
            public String fileName;
            public String fileUrl;
            public String md5Url;
            public String targetVersion;
        }
    }

    public static AssetInfo getAsset(String minecraftVersion, String loader) {
        AssetIndex targetIndex = getAssetIndex(minecraftVersion);
        if (targetIndex.convertFrom == null) {
            return createAsset(Collections.singletonList(getDownload(targetIndex, loader)), null);
        }

        return createAsset(targetIndex.convertFrom.stream()
                .map(AssetConfig::getAssetIndex)
                .map(it -> getDownload(it, loader))
                .collect(Collectors.toList()), targetIndex);
    }

    private static AssetIndex getAssetIndex(String minecraftVersion) {
        Version version = Version.from(minecraftVersion);
        return i18NAssetIndex.stream().filter(it -> {
            VersionRange range = new VersionRange(it.gameVersions);
            return range.contains(version);
        }).findFirst().orElseThrow(IllegalStateException::new);
    }

    private static AssetIndex.Download getDownload(AssetIndex index, String loader) {
        if (index.downloads == null) {
            throw new IllegalStateException("Download not found");
        }
        return index.downloads.stream()
                .filter(it -> it.loader.equalsIgnoreCase(loader)).findFirst().orElseGet(() -> index.downloads.get(0));
    }

    private static AssetInfo createAsset(List<AssetIndex.Download> download, @Nullable AssetIndex convert) {
        AssetInfo ret = new AssetInfo();

        ret.downloads = download.stream().map(it -> {
            AssetInfo.AssetDownloadInfo adi = new AssetInfo.AssetDownloadInfo();
            adi.fileName = it.filename;
            adi.fileUrl = CFPA_ASSET_ROOT + it.filename;
            adi.md5Url = CFPA_ASSET_ROOT + it.md5Filename;
            adi.targetVersion = it.targetVersion;
            return adi;
        }).collect(Collectors.toList());

        if (convert != null) {
            ret.covertPackFormat = convert.packFormat;
            ret.covertFileName =
                    String.format("Minecraft-Mod-Language-Modpack-Converted-%d.zip", convert.packFormat);
        }
        return ret;
    }

    private static class AssetIndex {
        String gameVersions;
        int packFormat;
        @Nullable List<String> convertFrom;
        @Nullable List<Download> downloads;

        private static class Download {
            String loader;
            String targetVersion;
            String filename;
            String md5Filename;
        }
    }
}
