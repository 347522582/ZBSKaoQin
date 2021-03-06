package net.zhongbenshuo.attendance.activity.version;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

import net.zhongbenshuo.attendance.R;
import net.zhongbenshuo.attendance.activity.base.SwipeBackActivity;
import net.zhongbenshuo.attendance.bean.User;
import net.zhongbenshuo.attendance.constant.ApkInfo;
import net.zhongbenshuo.attendance.contentprovider.SPHelper;
import net.zhongbenshuo.attendance.network.NetClient;
import net.zhongbenshuo.attendance.utils.ActivityController;
import net.zhongbenshuo.attendance.utils.BitmapUtils;
import net.zhongbenshuo.attendance.utils.GsonUtils;
import net.zhongbenshuo.attendance.utils.ZXingUtils;
import net.zhongbenshuo.attendance.widget.MyToolbar;

/**
 * 二维码分享页面
 * Created at 2019/3/1 11:16
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class QrCodeShareActivity extends SwipeBackActivity {

    private Context mContext;
    private ImageView ivQrCode;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_share);
        mContext = this;
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, R.string.ShareToOthers, R.drawable.back_white, R.drawable.icon_share_right, onClickListener);
        ivQrCode = findViewById(R.id.ivQrCode);
        filePath = getExternalFilesDir("QrCode") + File.separator + "share_qrcode" + ".jpg";
        createQrCode();
        ivQrCode.setOnLongClickListener(onLongClickListener);
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.iv_right:
                shareSingleImage(filePath);
                break;
            default:
                break;
        }
    };

    private View.OnLongClickListener onLongClickListener = (v) -> {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        BitmapUtils.saveImageToGallery(mContext, bitmap);
        showToast(R.string.SaveToGallerySuccessfully);
        return true;
    };

    /**
     * 生成专属二维码
     */
    private void createQrCode() {
        //二维码图片较大时，生成图片、保存文件的时间可能较长，因此放在新线程中
        showLoadingDialog(mContext, getString(R.string.GeneratingQRCode), true);
        new Thread(() -> {
            Bitmap logo;

            User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);

            if (!user.getIcon_url().equals("")) {
                String photoPath = (NetClient.getBaseUrl() + user.getIcon_url()).replace("\\", "/");
                logo = BitmapUtils.getBitmapFromNetwork(photoPath);
            } else {
                logo = BitmapUtils.getBitmapFromResource(mContext, R.mipmap.ic_launcher);
            }
            String downloadUrl = NetClient.getBaseUrlProject() + "VersionController/downloadNewVersionByLoginId.do?apkTypeId=" + ApkInfo.APK_TYPE_ID_ZBSAttendance
                    + "&loginId=" + user.getUser_id();
            boolean success = ZXingUtils.createQRImage(mContext, downloadUrl, 800, 800, logo, filePath);
            if (success) {
                runOnUiThread(() -> {
                    ivQrCode.setImageBitmap(BitmapFactory.decodeFile(filePath));
                    cancelDialog();
                });
            }
        }).start();
    }

    /**
     * 分享单张图片
     *
     * @param imagePath 图片路径
     */
    public void shareSingleImage(String imagePath) {
        Uri imageUri = Uri.fromFile(new File(imagePath));
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.ShareTo)));
    }

}
