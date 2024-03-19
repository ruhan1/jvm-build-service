package com.redhat.hacbs.container.analyser.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import jakarta.inject.Singleton;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.google.cloud.tools.jib.api.DescriptorDigest;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.image.json.BuildableManifestTemplate;
import com.google.cloud.tools.jib.image.json.ManifestTemplate;
import com.google.cloud.tools.jib.image.json.V21ManifestTemplate;
import com.google.cloud.tools.jib.registry.ManifestAndDigest;
import com.google.cloud.tools.jib.registry.RegistryClient;
import com.google.cloud.tools.jib.registry.credentials.CredentialRetrievalException;
import com.redhat.hacbs.classfile.tracker.NoCloseInputStream;
import com.redhat.hacbs.classfile.tracker.TrackingData;
import com.redhat.hacbs.container.analyser.deploy.containerregistry.ContainerUtil;

import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import picocli.CommandLine;

@CommandLine.Command(name = "image")
@Singleton
@Unremovable
public class AnalyseImage extends AnalyserBase {

    @CommandLine.Parameters(index = "0")
    String image;

    @CommandLine.Option(names = { "--base-image", "-b" }, required = false)
    Optional<String> baseImage;

    void doAnalysis(Set<String> gavs, Set<TrackingData> trackingData) throws Exception {
        Set<DescriptorDigest> layersToProcess = new HashSet<>();
        Log.infof("Processing image %s", image);
        RegistryClient client = extractLayers(image, layersToProcess::add);
        if (baseImage.isPresent()) {
            Log.infof("Processing base image %s", image);
            extractLayers(baseImage.get(), layersToProcess::remove);
        }
        Log.infof("Processing layers to extract: %s", layersToProcess);
        for (var layer : layersToProcess) {
            var blob = client.pullBlob(layer, s -> {
            }, s -> {
            });
            Path targetFile = Files.createTempFile("layer", "layer");

            try (OutputStream out = Files.newOutputStream(targetFile)) {
                blob.writeTo(out);
            }
            try (InputStream in = Files.newInputStream(targetFile)) {
                GZIPInputStream inputStream = new GZIPInputStream(in);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
                for (TarArchiveEntry entry = tarArchiveInputStream
                        .getNextEntry(); entry != null; entry = tarArchiveInputStream.getNextEntry()) {
                    Log.debugf("Processing %s from layer %s", entry.getName(), layer.getHash());
                    handleFile(entry.getName(), new NoCloseInputStream(tarArchiveInputStream), trackingData, gavs);
                }
            }
        }
    }

    RegistryClient extractLayers(String image, Consumer<DescriptorDigest> layerConsumer)
            throws InvalidImageReferenceException, IOException, RegistryException, CredentialRetrievalException {

        ImageReference imageReference = ImageReference.parse(image);
        RegistryClient registryClient = ContainerUtil.getRegistryClient(imageReference, null, false);
        ManifestAndDigest<ManifestTemplate> result = registryClient.pullManifest(imageReference.getQualifier());
        imageDigest = result.getDigest().toString();
        if (result.getManifest() instanceof V21ManifestTemplate) {
            V21ManifestTemplate template = (V21ManifestTemplate) result.getManifest();
            for (var layer : template.getLayerDigests()) {
                layerConsumer.accept(layer);
            }
        } else if (result.getManifest() instanceof BuildableManifestTemplate) {
            BuildableManifestTemplate template = (BuildableManifestTemplate) result.getManifest();
            for (var layer : template.getLayers()) {
                layerConsumer.accept(layer.getDigest());
            }
        }
        return registryClient;
    }
}
