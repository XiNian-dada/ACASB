package com.leeinx.acasb.controller;

import com.leeinx.acasb.dto.ApiResponse;
import com.leeinx.acasb.dto.DatasetImportFolderRequest;
import com.leeinx.acasb.dto.DatasetImportManifestRequest;
import com.leeinx.acasb.dto.DatasetImportResult;
import com.leeinx.acasb.entity.DatasetImageRecord;
import com.leeinx.acasb.service.DatasetImageRecordService;
import com.leeinx.acasb.service.DatasetImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataset")
@CrossOrigin(origins = "*")
public class DatasetController {
    private final DatasetImportService datasetImportService;
    private final DatasetImageRecordService datasetImageRecordService;

    public DatasetController(
            DatasetImportService datasetImportService,
            DatasetImageRecordService datasetImageRecordService) {
        this.datasetImportService = datasetImportService;
        this.datasetImageRecordService = datasetImageRecordService;
    }

    @PostMapping("/import-folder")
    public ResponseEntity<ApiResponse<DatasetImportResult>> importFolder(
            @RequestBody(required = false) DatasetImportFolderRequest request) {
        try {
            DatasetImportResult result = datasetImportService.importDatasetFolder(
                    request == null ? null : request.getDatasetPath(),
                    request == null ? null : request.getDatasetName(),
                    request == null ? null : request.getCopyImages()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "导入目录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/import-manifest")
    public ResponseEntity<ApiResponse<DatasetImportResult>> importManifest(
            @RequestBody DatasetImportManifestRequest request) {
        try {
            DatasetImportResult result = datasetImportService.importManifestPath(
                    request.getManifestPath(),
                    request.getDatasetName(),
                    request.getCopyImages()
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "导入 manifest 失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DatasetImportResult>> uploadManifest(
            @RequestParam("manifest") MultipartFile manifest,
            @RequestParam("images") MultipartFile[] images,
            @RequestParam(value = "dataset_name", required = false) String datasetName) {
        try {
            DatasetImportResult result = datasetImportService.importUploadedManifest(manifest, images, datasetName);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "上传 manifest 数据失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-record", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadRecord(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata,
            @RequestParam(value = "metadata_file", required = false) MultipartFile metadataFile,
            @RequestParam(value = "dataset_name", required = false) String datasetName,
            @RequestParam(value = "group_name", required = false) String groupName,
            @RequestParam(value = "group_relative_path", required = false) String groupRelativePath,
            @RequestParam(value = "schema_version", required = false) String schemaVersion,
            @RequestParam(value = "prompt_version", required = false) String promptVersion,
            @RequestParam(value = "generated_at", required = false) String generatedAt,
            @RequestParam(value = "api_interface", required = false) String apiInterface,
            @RequestParam(value = "model", required = false) String model) {
        try {
            String metadataJson = metadata;
            if ((metadataJson == null || metadataJson.isBlank()) && metadataFile != null && !metadataFile.isEmpty()) {
                metadataJson = new String(metadataFile.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            Map<String, Object> result = datasetImportService.uploadSingleRecord(
                    file,
                    metadataJson,
                    datasetName,
                    groupName,
                    groupRelativePath,
                    schemaVersion,
                    promptVersion,
                    generatedAt,
                    apiInterface,
                    model
            );
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "上传单条记录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listRecords(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) Boolean manualReview,
            @RequestParam(required = false) String analysisStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        long total = datasetImageRecordService.countFiltered(
                datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus, keyword
        );
        List<DatasetImageRecord> records = datasetImageRecordService.listFiltered(
                datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus, keyword, limit, offset
        );

        Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", total);
        result.put("limit", limit == null ? 20 : Math.min(limit, 200));
        result.put("offset", offset == null ? 0 : Math.max(offset, 0));
        result.put("note", "relativePath/groupRelativePath 为 ASCII 规范路径，originalRelativePath/originalGroupRelativePath 保留原始中文路径");
        result.put("items", records.stream().map(datasetImageRecordService::buildRecordView).toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecord(@PathVariable Long id) {
        DatasetImageRecord record = datasetImageRecordService.getRecordById(id);
        if (record == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "记录不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildRecordView(record)));
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverviewStats(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) Boolean manualReview,
            @RequestParam(required = false) String analysisStatus) {
        List<DatasetImageRecord> records = datasetImageRecordService.listForStats(
                datasetName, groupName, dynasty, province, rank, sceneType, manualReview, analysisStatus
        );
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildOverviewStats(records)));
    }

    @GetMapping("/stats/regions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegionStats(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) Boolean manualReview,
            @RequestParam(required = false) String analysisStatus) {
        List<DatasetImageRecord> records = datasetImageRecordService.listForStats(
                datasetName, groupName, dynasty, null, rank, null, manualReview, analysisStatus
        );
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildRegionStats(records)));
    }

    @GetMapping("/stats/colors")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getColorStats(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) Integer limit) {
        List<DatasetImageRecord> records = datasetImageRecordService.listForStats(
                datasetName, groupName, dynasty, province, rank, null, null, "success"
        );
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildColorStats(records, limit)));
    }

    @GetMapping("/stats/ranks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRankStats(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) Boolean manualReview) {
        List<DatasetImageRecord> records = datasetImageRecordService.listForStats(
                datasetName, groupName, dynasty, province, null, sceneType, manualReview, null
        );
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildRankStats(records)));
    }

    @GetMapping("/stats/styles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStyleStats(
            @RequestParam(required = false) String datasetName,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) Integer limit) {
        List<DatasetImageRecord> records = datasetImageRecordService.listForStats(
                datasetName, groupName, dynasty, province, rank, null, null, "success"
        );
        return ResponseEntity.ok(ApiResponse.success(datasetImageRecordService.buildStyleStats(records, limit)));
    }
}
