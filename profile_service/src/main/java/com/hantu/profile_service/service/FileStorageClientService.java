package com.hantu.profile_service.service;

import com.hantu.profile_service.dto.response.ApiResponse;
import com.hantu.profile_service.dto.response.FileUploadResponse;
import com.hantu.profile_service.exception.AppException;
import com.hantu.profile_service.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageClientService {
    private static final String AVATAR_CATEGORY = "avatar";
    private static final ParameterizedTypeReference<ApiResponse<FileUploadResponse>> UPLOAD_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Value("${app.services.file}")
    private String fileServiceBaseUrl;

    public String uploadAvatar(MultipartFile avatarFile, String authHeader) {
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("category", AVATAR_CATEGORY);
        multipartBody.add("file", asMultipartResource(avatarFile));

        try {
            ApiResponse<FileUploadResponse> response = RestClient.builder()
                    .baseUrl(fileServiceBaseUrl)
                    .build()
                    .post()
                    .uri("/files/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", authHeader)
                    .body(multipartBody)
                    .retrieve()
                    .body(UPLOAD_RESPONSE_TYPE);

            if (response == null || response.getResult() == null || response.getResult().relativePath() == null) {
                throw new AppException(ErrorCode.FILE_SERVICE_ERROR);
            }
            return response.getResult().relativePath();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.FILE_SERVICE_ERROR);
        }
    }

    public void deleteAvatarByRelativePath(String relativePath, String authHeader) {
        String fileName = extractFileName(relativePath);
        try {
            RestClient.builder()
                    .baseUrl(fileServiceBaseUrl)
                    .build()
                    .delete()
                    .uri(uriBuilder -> uriBuilder.path("/files")
                            .queryParam("category", AVATAR_CATEGORY)
                            .queryParam("fileName", fileName)
                            .build())
                    .header("Authorization", authHeader)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.FILE_SERVICE_ERROR);
        }
    }

    private ByteArrayResource asMultipartResource(MultipartFile avatarFile) {
        try {
            return new ByteArrayResource(avatarFile.getBytes()) {
                @Override
                public String getFilename() {
                    return avatarFile.getOriginalFilename();
                }
            };
        } catch (Exception exception) {
            throw new AppException(ErrorCode.FILE_SERVICE_ERROR);
        }
    }

    private String extractFileName(String relativePath) {
        String trimmed = relativePath == null ? "" : relativePath.trim();
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == trimmed.length() - 1) {
            throw new AppException(ErrorCode.FILE_SERVICE_ERROR);
        }
        // Only send leaf file name to file service delete endpoint.
        return trimmed.substring(lastSlash + 1);
    }
}
