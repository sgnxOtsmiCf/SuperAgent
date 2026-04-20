package cn.sgnxotsmicf.common.auth;

/**
 * 角色常量类
 * 对应数据库表：superagent_role
 * @author lixiang
 * @date 2026/3/31
 */
public final class RoleConstant {

    // 私有构造，禁止实例化
    private RoleConstant() {}

    // ===================== 角色ID =====================
    /** 管理员ID */
    public static final Long ADMIN_ID = 1L;
    /** 访客ID */
    public static final Long GUEST_ID = 2L;
    /** 普通用户ID */
    public static final Long USER_ID = 3L;
    /** 会员用户ID */
    public static final Long MEMBER_ID = 4L;

    // ===================== 角色编码 =====================
    /** 管理员编码 */
    public static final String ADMIN_CODE = "admin";
    /** 访客编码 */
    public static final String GUEST_CODE = "guest";
    /** 普通用户编码 */
    public static final String USER_CODE = "user";
    /** 会员用户编码 */
    public static final String MEMBER_CODE = "member";

    // ===================== 角色名称 =====================
    /** 管理员名称 */
    public static final String ADMIN_NAME = "管理员";
    /** 访客名称 */
    public static final String GUEST_NAME = "访客";
    /** 普通用户名称 */
    public static final String USER_NAME = "普通用户";
    /** 会员用户名称 */
    public static final String MEMBER_NAME = "会员用户";

}