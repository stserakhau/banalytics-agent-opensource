package com.banalytics.box.web.sec;

import com.banalytics.box.module.AbstractTask;
import com.banalytics.box.service.EngineService;
import com.banalytics.box.service.SystemThreadsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipOutputStream;

import static com.banalytics.box.module.cloud.portal.suc.DownloadUtil.unzip;
import static com.banalytics.box.module.cloud.portal.suc.DownloadUtil.zipFolder;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/secured/system")
@Slf4j
public class SystemRest {

    private final EngineService engineService;

    @GetMapping("/restart")
    @ResponseStatus(HttpStatus.OK)
    public void restart() throws Exception {
        log.info("Reboot initiated via local console");
        engineService.reboot();
    }

    @GetMapping("/download-configuration")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> downloadConfiguration() throws Exception {
        log.info("Download configuration");

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            File folder = engineService.applicationConfigFolder();
            if (folder.exists() && folder.isDirectory()) {
                zipFolder(folder, folder.getName(), zipOutputStream);
            }

            zipOutputStream.finish();

            HttpHeaders headers = new HttpHeaders();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            headers.add("Content-Disposition", "attachment; filename=instance-config-" + sdf.format(new Date()) + ".zip");

            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload-configuration")
    public ResponseEntity<String> uploadConfiguration(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Upload configuration");

        try {
            for (AbstractTask<?> instance : engineService.instances()) {
                instance.stop();
            }
            // Save uploaded file to a temporary location
            File tempZipFile = File.createTempFile("uploaded", ".zip");
            try (FileOutputStream fos = new FileOutputStream(tempZipFile)) {
                fos.write(file.getBytes());
            }

            File configFolder = engineService.applicationConfigFolder();
            FileUtils.cleanDirectory(configFolder);

            File configParentFolder = configFolder.getParentFile();
            // Unpack the ZIP file
            unzip(tempZipFile, configParentFolder);

            // Clean up the temporary file
            tempZipFile.delete();

            SystemThreadsService.execute(this, ()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    engineService.reboot();
                }
            });
            return ResponseEntity.ok("Configuration uploaded and unpacked successfully! System rebooted. Wait a minute.");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload or unpack the ZIP file: " + e.getMessage());
        }
    }

    @GetMapping("/reset-configuration")
    public ResponseEntity<String> resetConfiguration() throws Exception {
        log.info("Reset configuration");
        for (AbstractTask<?> instance : engineService.instances()) {
            instance.stop();
        }
        File configFolder = engineService.applicationConfigFolder();
        File instancesFolder = new File(configFolder, "instances");
        FileUtils.cleanDirectory(instancesFolder);

        SystemThreadsService.execute(this, ()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                engineService.reboot();
            }
        });

        return ResponseEntity.ok("Configuration was reset! System rebooted. Wait a minute.");
    }
}