package cn.sgnxotsmicf.config;

import com.aliyun.tea.TeaException;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 18:05
 * @Version: 1.0
 * @Description: 阿里云短信验证码配置（号码认证服务）
 */
@Slf4j
@Configuration
public class SmsConfig {

    @Value("${alibaba.cloud.access-key}")
    private String accessKeyId;

    @Value("${alibaba.cloud.secret-key}")
    private String accessKeySecret;

    /**
     * <b>description</b> :
     * <p>使用凭据初始化账号Client</p>
     * @return Client
     *
     * @throws Exception
     */
    public com.aliyun.dypnsapi20170525.Client createClient() throws Exception {
        // 工程代码建议使用更安全的无AK方式，凭据配置方式请参见：https://help.aliyun.com/document_detail/378657.html。
        com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setCredential(credential)
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        // Endpoint 请参考 https://api.aliyun.com/product/Dypnsapi
        config.endpoint = "dypnsapi.aliyuncs.com";
        return new com.aliyun.dypnsapi20170525.Client(config);
    }

    /**
     * 发送短信验证码，并【返回获取到的验证码】
     * @param phone 接收验证码的手机号
     * @return 验证码字符串（6位数字）
     */
    public String sendSmsPhoneCode(String phone){
        com.aliyun.dypnsapi20170525.Client client = null;
        try {
            client = createClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                .setSignName("速通互联验证码")
                .setTemplateCode("100001")
                .setPhoneNumber(phone)
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                .setSchemeName("登录注册")
                .setCodeLength(6L)
                .setValidTime(300L)
                .setInterval(60L)
                .setReturnVerifyCode(true);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCodeWithOptions(sendSmsVerifyCodeRequest, runtime);
            log.info("阿里云短信验证码响应：{}", new Gson().toJson(resp));
            String verifyCode = resp.getBody().getModel().verifyCode;
            if (!StringUtils.hasText(verifyCode)) {
                throw new RuntimeException("获取验证码失败，阿里云未返回验证码");
            }
            return verifyCode;
        } catch (TeaException error) {
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            log.error(error.getData().get("Recommend").toString());
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            log.error(error.getData().get("Recommend").toString());
        }
        return "";
    }

}
