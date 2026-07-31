package app.mealdeck.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.mealdeck.dto.PackingSlipResponse;
import app.mealdeck.service.PackingSlipExtractionService;

/** Exposes review-only packing-slip extraction over HTTP. */
@RestController
@RequestMapping("/api/packing-slips")
public class PackingSlipController {
    private final PackingSlipExtractionService extraction;

    /**
     * @param extraction packing-slip extraction service
     */
    public PackingSlipController(PackingSlipExtractionService extraction) {
        this.extraction = extraction;
    }

    /**
     * @param slip packing-slip photo
     * @return extracted and template-matched manifest
     */
    @PostMapping("/extract")
    public PackingSlipResponse extract(@RequestPart("slip") MultipartFile slip) {
        return extraction.extract(slip);
    }
}
