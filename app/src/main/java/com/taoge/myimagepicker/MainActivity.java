package com.taoge.myimagepicker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.taoge.ListImgDirPopupWindow;
import com.taoge.myimagepicker.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {



    private GridView mGridView;
    private List<String> mImgs;
    private ImageAdapter mImgAdapter;

    private RelativeLayout mBottomLy;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    private ProgressDialog mProgressDialog;


    private static final int DATA_LOADED = 0X110;

    private ListImgDirPopupWindow mDirPopupWindow;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {

                mProgressDialog.dismiss();
                //绑定数据到View中
                data2View();

                initDirPopupWindow();
            }

        }
    };






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            Toast.makeText(this, "必须获得该权限才能使用此功能！", Toast.LENGTH_SHORT).show();
        } else {

            initData();
            initEvent();
        }


    }


    private void initView() {
        mGridView = findViewById(R.id.gridView);
        mBottomLy = findViewById(R.id.bottom_ly);
        mDirName = findViewById(R.id.dir_name);
        mDirCount = findViewById(R.id.dir_count);


    }


    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initData() {

        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用！", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");

        new Thread() {
            @Override
            public void run() {

                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "=?or "
                                + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths = new HashSet<>();

                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }

                    if (parentFile.list() == null)
                        continue;

                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String fileName) {
                            if (fileName.endsWith(".jpg")
                                    || fileName.endsWith(".jpeg")
                                    || fileName.endsWith(".png"))
                                return true;

                            return false;
                        }
                    }).length;

                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }

                cursor.close();

                mHandler.sendEmptyMessage(DATA_LOADED);

            }

        }.start();

    }

    private void initEvent() {

        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                mDirPopupWindow.showAsDropDown(mBottomLy,0,0);
                lightOff();

            }
        });
    }




    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    private void initDirPopupWindow() {
        mDirPopupWindow=new ListImgDirPopupWindow(this,mFolderBeans);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        mDirPopupWindow.setOnDirSelectedListener(new ListImgDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean bean) {
                mCurrentDir=new File(bean.getDir());
                mImgs= Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String fileName) {
                        if (fileName.endsWith(".jpg")
                                || fileName.endsWith(".jpeg")
                                || fileName.endsWith(".png"))
                            return true;

                        return false;
                    }
                }));

                mImgAdapter=new ImageAdapter(MainActivity.this,mImgs,mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);
                mDirCount.setText(mImgs.size()+"");
                mDirName.setText(bean.getName());

                mDirPopupWindow.dismiss();

            }
        });
    }

    /**
     * 内容区域变亮
     */
    private void lightOn() {

        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.alpha=1.0f;
        getWindow().setAttributes(lp);
    }

    private void lightOff() {
        WindowManager.LayoutParams lp=getWindow().getAttributes();
        lp.alpha=0.3f;
        getWindow().setAttributes(lp);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initData();
                    initEvent();
                }else {
                    Toast.makeText(this, "发生未知错误!", Toast.LENGTH_SHORT).show();
                }
                break;


            default:
                break;
        }
    }



}
