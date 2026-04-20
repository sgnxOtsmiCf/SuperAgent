package cn.sgnxotsmicf.common.tools;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.exception.AgentException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MinioUtil {

    @Resource
    private MinioClient superAgentMinioClient;

    /**
     * 默认桶名称（从配置文件读取）
     */
    private final String defaultBucketName = "superagent";

    /**
     * 使用默认桶创建（无参）
     */
    public void createBucketIfNotExists() throws Exception {
        createBucketIfNotExists(defaultBucketName);
    }

    /**
     * 手动指定桶创建
     * @param bucketName 自定义桶名称
     */
    public void createBucketIfNotExists(String bucketName){
        try {
            boolean exists = superAgentMinioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                superAgentMinioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
            }
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.BUCKET_CREATE_FAIL);
        }
    }

    /**
     * 使用默认桶上传文件
     * @param file 上传的文件
     * @return 文件访问地址
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, defaultBucketName);
    }

    /**
     * 手动指定桶上传文件
     * @param file 上传的文件
     * @param bucketName 自定义桶名称
     * @return 文件访问地址
     */
    public String uploadFile(MultipartFile file, String bucketName){
        // 先创建桶
        createBucketIfNotExists(bucketName);

        // 生成唯一文件名，防止重名
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = "/" + DateUtil.today() + "/" + UUID.randomUUID().toString(true)+ suffix;

        // 上传
        try (InputStream inputStream = file.getInputStream()) {
            superAgentMinioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }catch (Exception e) {
            log.error("MinIO文件上传失败，原始异常：", e);
            throw new AgentException(ResultCodeEnum.FILE_UPLOAD_FAIL);
        }

        // 返回文件预览地址
        return getFileUrl(fileName, bucketName);
    }

    /**
     * 使用默认桶上传字节内容（前后端分离场景，避免本地目录交互）
     * @param fileBytes 文件字节数组
     * @param originalFilename 原始文件名（用于推断后缀）
     * @param contentType MIME 类型
     * @return 文件访问地址
     */
    public String uploadBytes(byte[] fileBytes, String originalFilename, String contentType) {
        return uploadBytes(fileBytes, originalFilename, contentType, defaultBucketName);
    }

    /**
     * 手动指定桶上传字节内容（前后端分离场景，避免本地目录交互）
     * @param fileBytes 文件字节数组
     * @param originalFilename 原始文件名（用于推断后缀）
     * @param contentType MIME 类型
     * @param bucketName 自定义桶名称
     * @return 文件访问地址
     */
    public String uploadBytes(byte[] fileBytes, String originalFilename, String contentType, String bucketName) {
        createBucketIfNotExists(bucketName);

        String safeName = originalFilename == null ? "file.bin" : originalFilename;
        String suffix = ".bin";
        int dotIndex = safeName.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
            suffix = safeName.substring(dotIndex);
        }
        String fileName = DateUtil.today() + "/" + UUID.randomUUID().toString(true) + suffix;

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            superAgentMinioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, fileBytes.length, -1)
                            .contentType(contentType)
                            .headers(Map.of("Content-Disposition", "attachment; filename=\"" + safeName + "\""))
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO字节内容上传失败，原始异常：", e);
            throw new AgentException(ResultCodeEnum.FILE_UPLOAD_FAIL);
        }

        return getFileUrl(fileName, bucketName);
    }

    /**
     * 使用默认桶获取文件地址
     * @param fileName 文件名
     * @return 临时访问地址
     */
    public String getFileUrl(String fileName) throws Exception {
        return getFileUrl(fileName, defaultBucketName);
    }

    /**
     * 手动指定桶获取文件地址
     * @param fileName 文件名
     * @param bucketName 自定义桶名称
     * @return 临时访问地址
     */
    public String getFileUrl(String fileName, String bucketName){
        try {
            return superAgentMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(7, java.util.concurrent.TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.FILE_GET_URL_FAIL);
        }
    }

    /**
     * 使用默认桶下载文件
     * @param fileName 文件名
     * @return 文件流
     */
    public InputStream downloadFile(String fileName){
        return downloadFile(fileName, defaultBucketName);
    }

    /**
     * 手动指定桶下载文件
     * @param fileName 文件名
     * @param bucketName 自定义桶名称
     * @return 文件流
     */
    public InputStream downloadFile(String fileName, String bucketName){
        try {
            return superAgentMinioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.FILE_DOWNLOAD_FAIL);
        }
    }

    /**
     * 使用默认桶删除文件
     * @param fileName 文件名
     */
    public void deleteFile(String fileName){
        deleteFile(fileName, defaultBucketName);
    }

    /**
     * 手动指定桶删除文件
     * @param fileName 文件名
     * @param bucketName 自定义桶名称
     */
    public void deleteFile(String fileName, String bucketName){
        try {
            superAgentMinioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.FILE_DELETE_FAIL);
        }
    }

    /**
     * 查询所有桶
     */
    public List<Bucket> listBuckets(){
        try {
            return superAgentMinioClient.listBuckets();
        } catch (Exception e) {
            throw new AgentException(ResultCodeEnum.BUCKET_SELECT_FAIL);
        }
    }
}