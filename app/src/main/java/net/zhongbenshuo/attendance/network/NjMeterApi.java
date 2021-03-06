package net.zhongbenshuo.attendance.network;

import java.util.Map;

import net.zhongbenshuo.attendance.bean.DepartmentResult;
import net.zhongbenshuo.attendance.bean.LoginResult;
import net.zhongbenshuo.attendance.bean.NormalResult;
import net.zhongbenshuo.attendance.bean.UserOperateResult;
import net.zhongbenshuo.attendance.bean.version.VersionLog;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Retrofit网络请求构建接口
 * Created at 2018/11/28 13:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public interface NjMeterApi {

    /**
     * 主账号注册的请求
     *
     * @param params 参数
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("user/register.do")
    Observable<UserOperateResult> register(@FieldMap Map<String, Object> params);

    /**
     * 主账号登录的请求
     *
     * @param params 参数
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("user/login.do")
    Observable<LoginResult> login(@FieldMap Map<String, Object> params);

    /**
     * 更新用户信息
     *
     * @param params 参数
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("user/updateInfo.do")
    Observable<UserOperateResult> updateInfo(@FieldMap Map<String, Object> params);

    /**
     * 更新语音服务器
     *
     * @param params 参数
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("user/updateVoiceServer.do")
    Observable<UserOperateResult> updateVoiceServer(@FieldMap Map<String, Object> params);

    /**
     * 更新消息服务器
     *
     * @param params 参数
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("user/updateMessageServer.do")
    Observable<UserOperateResult> updateMessageServer(@FieldMap Map<String, Object> params);

    /**
     * 更新用户信息
     *
     * @return 返回值
     */
    @POST("user/searchDepartment.do")
    Observable<DepartmentResult> searchDepartment();

    /**
     * 更新用户头像
     *
     * @param information 描述信息
     * @param file        头像文件
     * @return 更新结果
     */
    @Multipart
    @POST("user/uploadHeadPortrait.do")
    Observable<NormalResult> uploadUserIcon(@Part("information") RequestBody information, @Part MultipartBody.Part file);

    /**
     * 软件历史版本更新日志信息
     *
     * @return 返回值
     */
    @FormUrlEncoded
    @POST("VersionController/getNewVersionUpdateLog.do")
    Observable<VersionLog> getVersionLog(@FieldMap Map<String, Object> params);

    /**
     * 上传错误日志文件
     *
     * @param file 错误日志文件
     * @return 上传结果
     */
    @Multipart
    @POST("AndroidController/uploadAndroidLog.do")
    Observable<NormalResult> uploadCrashFiles(@Part MultipartBody.Part file);

    /**
     * 下载软件
     *
     * @param params 文件类型
     * @return ResponseBody
     */
    @FormUrlEncoded
    @POST("VersionController/downloadNewVersion.do")
    Call<ResponseBody> downloadFile(@FieldMap Map<String, Object> params);

    /**
     * 下载软件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    @GET
    Call<ResponseBody> downloadFile(@Url String filePath);

}
