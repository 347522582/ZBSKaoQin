package net.zhongbenshuo.attendance.activity.version;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.zhongbenshuo.attendance.BuildConfig;
import net.zhongbenshuo.attendance.R;
import net.zhongbenshuo.attendance.activity.base.SwipeBackActivity;
import net.zhongbenshuo.attendance.adapter.VersionLogAdapter;
import net.zhongbenshuo.attendance.bean.version.Version;
import net.zhongbenshuo.attendance.bean.version.VersionLog;
import net.zhongbenshuo.attendance.constant.ApkInfo;
import net.zhongbenshuo.attendance.constant.Constants;
import net.zhongbenshuo.attendance.network.ExceptionHandle;
import net.zhongbenshuo.attendance.network.NetClient;
import net.zhongbenshuo.attendance.network.NetworkSubscriber;
import net.zhongbenshuo.attendance.utils.ActivityController;
import net.zhongbenshuo.attendance.utils.ApkUtils;
import net.zhongbenshuo.attendance.utils.FileUtil;
import net.zhongbenshuo.attendance.utils.LogUtils;
import net.zhongbenshuo.attendance.utils.MathUtils;
import net.zhongbenshuo.attendance.utils.NetworkUtil;
import net.zhongbenshuo.attendance.widget.MyProgressBar;
import net.zhongbenshuo.attendance.widget.MyToolbar;
import net.zhongbenshuo.attendance.widget.RecyclerViewDivider;
import net.zhongbenshuo.attendance.widget.dialog.CommonWarningDialog;
import net.zhongbenshuo.attendance.widget.dialog.DownLoadDialog;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * App历史版本页面
 * Created at 2019/3/1 11:15
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class VersionsActivity extends SwipeBackActivity {

    private Context mContext;

    private RecyclerView lvVersionLog;
    private String versionFileName, latestVersionMD5, apkDownloadPath;
    private MyProgressBar myProgressBar;
    private TextView tvCompletedSize, tvTotalSize;
    private float apkSize, completedSize;

    private static final int GET_UNKNOWN_APP_SOURCES = 1;
    protected static final int INSTALL_PACKAGES_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mContext = this;
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, R.string.VersionView, R.drawable.back_white, onClickListener);
        ((TextView) findViewById(R.id.tv_version)).setText(ApkUtils.getVersionName(this));

        lvVersionLog = findViewById(R.id.lv_versionLog);
        //设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        lvVersionLog.setLayoutManager(linearLayoutManager);
        lvVersionLog.addItemDecoration(new RecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL, 2, ContextCompat.getColor(this, R.color.gray_slight)));

        Map<String, Object> params = new HashMap<>(2);
        params.put("apkTypeId", ApkInfo.APK_TYPE_ID_ZBSAttendance);
        Observable<VersionLog> versionLogCall = NetClient.getInstances(NetClient.getBaseUrlProject()).getNjMeterApi().getVersionLog(params);
        versionLogCall.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkSubscriber<VersionLog>(mContext, getClass().getSimpleName()) {

            @Override
            public void onStart() {
                super.onStart();
                //接下来可以检查网络连接等操作
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    showToast(getString(R.string.NetworkUnavailable));
                    if (!isUnsubscribed()) {
                        unsubscribe();
                    }
                } else {
                    showLoadingDialog(mContext, getString(R.string.Searching), true);
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                cancelDialog();
                showToast(responseThrowable.message);
            }

            @Override
            public void onNext(VersionLog versionLog) {
                cancelDialog();
                if (versionLog == null || versionLog.getVersion().size() < 1) {
                    showToast("版本更新日志获取失败");
                } else {
                    List<Version> versionList = versionLog.getVersion();
                    VersionLogAdapter versionLogAdapter = new VersionLogAdapter(VersionsActivity.this, versionList);
                    lvVersionLog.setAdapter(versionLogAdapter);
                    versionLogAdapter.buttonSetOnclick((view, position) -> {
                        versionFileName = versionList.get(position).getVersionFileName();
                        latestVersionMD5 = versionList.get(position).getMd5Value();
                        apkDownloadPath = versionList.get(position).getVersionUrl().replace("\\", "/");
                        LogUtils.d("DownloadPath", apkDownloadPath);
                        if (isDownloaded()) {
                            //如果已经下载了，则弹出窗口询问直接安装还是重新下载
                            showReDownloadWarningDialog();
                        } else {
                            downloadApk();
                        }
                    });
                }
            }
        });
    }

    /**
     * 提示用户重复下载的弹窗
     */
    private void showReDownloadWarningDialog() {
        CommonWarningDialog commonWarningDialog = new CommonWarningDialog(mContext, getString(R.string.warning_redownload));
        commonWarningDialog.setButtonText(getString(R.string.DownloadAgain), getString(R.string.InstallDirectly));
        commonWarningDialog.setCancelable(false);
        commonWarningDialog.setOnDialogClickListener(new CommonWarningDialog.OnDialogClickListener() {
            @Override
            public void onOKClick() {
                //直接安装
                File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                checkIsAndroidO(file);
            }

            @Override
            public void onCancelClick() {
                //下载最新的版本程序
                downloadApk();
            }
        });
        commonWarningDialog.show();
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            default:
                break;
        }
    };

    /**
     * 判断是否已经下载过该文件
     *
     * @return boolean
     */
    private boolean isDownloaded() {
        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + versionFileName);
        LogUtils.d("MD5", file.getPath());
        return file.isFile() && latestVersionMD5.equals(FileUtil.getFileMD5(file));
    }

    /**
     * 下载新版本程序
     */
    private void downloadApk() {
        if (apkDownloadPath.equals(Constants.EMPTY)) {
            showToast("下载路径有误，请联系客服");
        } else {
            DownLoadDialog progressDialog = new DownLoadDialog(mContext);
            myProgressBar = progressDialog.findViewById(R.id.progressbar_download);
            tvCompletedSize = progressDialog.findViewById(R.id.tv_completedSize);
            tvTotalSize = progressDialog.findViewById(R.id.tv_totalSize);
            progressDialog.setCancelable(false);
            progressDialog.show();
            NetClient.downloadFileProgress(apkDownloadPath, (currentBytes, contentLength, done) -> {
                //获取到文件的大小
                apkSize = MathUtils.formatFloat((float) contentLength / 1024f / 1024f, 2);
                tvTotalSize.setText(String.format(mContext.getString(R.string.file_size_m), String.valueOf(apkSize)));
                //已完成大小
                completedSize = MathUtils.formatFloat((float) currentBytes / 1024f / 1024f, 2);
                tvCompletedSize.setText(String.format(mContext.getString(R.string.file_size_m), String.valueOf(completedSize)));
                myProgressBar.setProgress(MathUtils.formatFloat(completedSize / apkSize * 100, 1));
                if (done) {
                    progressDialog.dismiss();
                }
            }, new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                    //处理下载文件
                    if (response.body() != null) {
                        try {
                            InputStream is = response.body().byteStream();
                            //定义下载后文件的路径和名字，例如：/Download/JiangSuMetter_1.0.1.apk
                            File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + versionFileName);
                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(is);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = bis.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            bis.close();
                            is.close();
                            checkIsAndroidO(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("下载出错，" + e.getMessage() + "，请联系管理员");
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                    progressDialog.dismiss();
                    showToast("下载出错，" + t.getMessage() + "，请联系管理员");
                }
            });
        }
    }

    /**
     * 安装apk
     *
     * @param file 需要安装的apk
     */
    private void installApk(File file) {
        //先验证文件的正确性和完整性（通过MD5值）
        if (file.isFile() && latestVersionMD5.equals(FileUtil.getFileMD5(file))) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileProvider", file);//在AndroidManifest中的android:authorities值
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            startActivity(intent);
        } else {
            showToast("文件异常，无法安装");
        }
    }

    /**
     * Android8.0需要处理未知应用来源权限问题,否则直接安装
     */
    private void checkIsAndroidO(File file) {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean b = getPackageManager().canRequestPackageInstalls();
            if (b) {
                installApk(file);
            } else {
                //请求安装未知应用来源的权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUEST_CODE);
            }
        } else {
            installApk(file);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            LogUtils.d(TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        switch (requestCode) {
            case INSTALL_PACKAGES_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                    installApk(file);
                } else {
                    //  Android8.0以上引导用户手动开启安装权限
                    if (Build.VERSION.SDK_INT >= 26) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GET_UNKNOWN_APP_SOURCES:
                    // 从安装未知来源文件的设置页面返回
                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                    checkIsAndroidO(file);
                    break;
                default:
                    break;
            }
        }
    }
}
