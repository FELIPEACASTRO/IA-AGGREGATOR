package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.port.in.ModelCatalogUseCase;
import com.ia.aggregator.application.ai.port.in.ModelCatalogUseCase.ModelDto;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/models")
public class ModelCatalogController {

    private final ModelCatalogUseCase modelCatalogUseCase;

    public ModelCatalogController(ModelCatalogUseCase modelCatalogUseCase) {
        this.modelCatalogUseCase = modelCatalogUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ModelDto>>> getActiveModels() {
        return ResponseEntity.ok(ApiResponse.ok(modelCatalogUseCase.getActiveModels()));
    }

    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<List<ModelDto>>> getDefaultModels() {
        return ResponseEntity.ok(ApiResponse.ok(modelCatalogUseCase.getDefaultModels()));
    }
}
