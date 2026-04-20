package cn.sgnxotsmicf.common.auth;

/**
 * 权限常量类
 * 对应数据库表：superagent_permission
 * @author lixiang
 * @date 2026/3/31
 */
public final class PermissionConstant {

    // 私有构造，禁止实例化
    private PermissionConstant() {}

    // ===================== 权限ID =====================
    /** 超级智能体使用权限ID */
    public static final Long AGENT_SUPER_USE_ID = 1L;
    /** 单场景智能体使用权限ID */
    public static final Long AGENT_SCENE_USE_ID = 2L;
    /** token无限制使用权限ID */
    public static final Long TOKEN_UNLIMITED_ID = 3L;
    /** token每日100万权限ID */
    public static final Long TOKEN_DAILY_LIMIT_ID = 4L;
    /** 文件传入权限ID */
    public static final Long FILE_UPLOAD_ID = 5L;
    /** 文件下载权限ID */
    public static final Long FILE_DOWNLOAD_ID = 6L;
    /** 多模态使用权限ID */
    public static final Long AGENT_MULTIMODAL_USE_ID = 7L;

    // ===================== 权限编码 =====================
    /** 超级智能体使用权限编码 */
    public static final String AGENT_SUPER_USE_CODE = "agent:super:use";
    /** 单场景智能体使用权限编码 */
    public static final String AGENT_SCENE_USE_CODE = "agent:scene:use";
    /** token无限制使用权限编码 */
    public static final String TOKEN_UNLIMITED_CODE = "token:limit:unlimited";
    /** token每日100万权限编码 */
    public static final String TOKEN_DAILY_LIMIT_CODE = "token:limit:daily";
    /** 文件传入权限编码 */
    public static final String FILE_UPLOAD_CODE = "file:upload";
    /** 文件下载权限编码 */
    public static final String FILE_DOWNLOAD_CODE = "file:download";
    /** 多模态使用权限编码 */
    public static final String AGENT_MULTIMODAL_USE_CODE = "agent:multimodal:use";

    // ===================== 权限名称 =====================
    /** 超级智能体使用权限名称 */
    public static final String AGENT_SUPER_USE_NAME = "超级智能体使用权限";
    /** 单场景智能体使用权限名称 */
    public static final String AGENT_SCENE_USE_NAME = "单场景智能体使用权限";
    /** token无限制使用权限名称 */
    public static final String TOKEN_UNLIMITED_NAME = "token无限制使用权限";
    /** token每日100万权限名称 */
    public static final String TOKEN_DAILY_LIMIT_NAME = "token每日100万权限";
    /** 文件传入权限名称 */
    public static final String FILE_UPLOAD_NAME = "文件传入权限";
    /** 文件下载权限名称 */
    public static final String FILE_DOWNLOAD_NAME = "文件下载权限";
    /** 多模态使用权限名称 */
    public static final String AGENT_MULTIMODAL_USE_NAME = "多模态使用权限";

}