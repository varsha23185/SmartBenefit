package com.smartbenefit.controller;

import com.smartbenefit.service.SchemeImportService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Administrator screen for loading schemes from a CSV file (access is limited to ROLE_ADMIN by SecurityConfig). */
@Controller
@RequestMapping("/admin/import")
public class AdminImportController {

    private static final String BUNDLED_FILE = "import/central-schemes.csv";

    private final SchemeImportService importService;

    public AdminImportController(SchemeImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public String page() {
        return "admin-import";
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Please choose a CSV file first.");
            return "admin-import";
        }
        try (InputStream in = file.getInputStream()) {
            model.addAttribute("result", importService.importCsv(in));
        } catch (IOException e) {
            model.addAttribute("error", "The file could not be read.");
        }
        return "admin-import";
    }

    /** Imports the verified central schemes that ship with the application (same code path as an upload). */
    @PostMapping("/bundled")
    public String bundled(Model model) {
        try (InputStream in = new ClassPathResource(BUNDLED_FILE).getInputStream()) {
            model.addAttribute("result", importService.importCsv(in));
        } catch (IOException e) {
            model.addAttribute("error", "The bundled scheme file could not be read.");
        }
        return "admin-import";
    }

    /** Downloads the bundled file, which also serves as a template for the administrator's own files. */
    @GetMapping("/sample")
    public ResponseEntity<byte[]> sample() throws IOException {
        byte[] data;
        try (InputStream in = new ClassPathResource(BUNDLED_FILE).getInputStream()) {
            data = in.readAllBytes();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"central-schemes.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }
}
