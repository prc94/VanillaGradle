/*
 * This file is part of VanillaGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
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
 */
package org.spongepowered.gradle.vanilla.internal.repository.modifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.parchmentmc.feather.io.gson.MDCGsonAdapterFactory;
import org.parchmentmc.feather.io.gson.NamedAdapter;
import org.parchmentmc.feather.io.gson.OffsetDateTimeAdapter;
import org.parchmentmc.feather.io.gson.SimpleVersionAdapter;
import org.parchmentmc.feather.io.gson.metadata.MetadataAdapterFactory;
import org.parchmentmc.feather.mapping.VersionedMappingDataContainer;
import org.parchmentmc.feather.named.Named;
import org.parchmentmc.feather.util.SimpleVersion;
import org.spongepowered.gradle.vanilla.internal.resolver.AsyncUtils;
import org.spongepowered.gradle.vanilla.internal.transformer.ParchmentRenamingTransformer;
import org.spongepowered.gradle.vanilla.repository.MinecraftResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Formatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ParchmentModifier implements ArtifactModifier {

    private final Project project;
    private final Dependency parchmentDep;

    public ParchmentModifier(Project project, String parchmentSpec) {
        this.project = project;
        this.parchmentDep = project.getDependencies().create(parchmentSpec);
    }

    @Override
    public String key() {
        return "pmc";
    }

    @Override
    public String stateKey() {
        return parchmentDep.getVersion() != null ?
                //dots in state key for some reason are causing dependency not to resolve properly
                parchmentDep.getVersion().replace(".", "")
                : "noVer";
    }

    @Override
    public CompletableFuture<TransformerProvider> providePopulator(MinecraftResolver.Context context) {
        final Path parchmentPath;

        try {
            parchmentPath = resolveParchment(project, parchmentDep);
        } catch (IOException e) {
            return AsyncUtils.failedFuture(e);
        }

        return AsyncUtils.failableFuture(() -> {
            VersionedMappingDataContainer mappings;

            try (ZipFile zipFile = new ZipFile(parchmentPath.toFile())) {
                ZipEntry zipFileEntry = zipFile.getEntry(PARCHMENT_JSON_FILENAME);
                Objects.requireNonNull(zipFileEntry, new Formatter()
                        .format("Could not find %s in parchment zip archive", PARCHMENT_JSON_FILENAME).toString());

                try (Reader reader = new InputStreamReader(zipFile.getInputStream(zipFileEntry))) {
                    mappings = GSON.fromJson(reader, VersionedMappingDataContainer.class);
                }
            }

            return () -> new ParchmentRenamingTransformer(mappings);
        }, context.executor());
    }

    @Override
    public boolean requiresLocalStorage() {
        return false;
    }

    private static final String PARCHMENT_JSON_FILENAME = "parchment.json";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new MDCGsonAdapterFactory())
            .registerTypeAdapter(SimpleVersion.class, new SimpleVersionAdapter())
            .registerTypeAdapterFactory(new MetadataAdapterFactory())
            .registerTypeAdapter(Named.class, new NamedAdapter())
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    private static Path resolveParchment(Project project, Dependency parchmentDep) throws IOException {
        return project.getConfigurations()
                .detachedConfiguration(parchmentDep)
                .getSingleFile().toPath();
    }
}