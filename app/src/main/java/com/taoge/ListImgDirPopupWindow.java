package com.taoge;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.taoge.myimagepicker.R;
import com.taoge.myimagepicker.bean.FolderBean;
import com.taoge.myimagepicker.util.ImageLoader;

import java.util.List;

/**
 * created by：TangTao on 2018/10/13 15:32
 * <p>
 * email：xxx@163.com
 */
public class ListImgDirPopupWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private ListView mListView;
    private View mConvertView;
    private List<FolderBean> mDatas;

    public OnDirSelectedListener mListener;

    public interface OnDirSelectedListener{
        void onSelected(FolderBean bean);
    }

    public void setOnDirSelectedListener(OnDirSelectedListener listener) {
        this.mListener = listener;
    }

    public ListImgDirPopupWindow(Context context, List<FolderBean> datas) {

        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
        mDatas = datas;

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });


        initView(context);
        initEvent();
    }


    private void initView(Context context) {

        mListView = mConvertView.findViewById(R.id.list_dir);
        mListView.setAdapter(new ListDirAdapter(context,mDatas));

    }


    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if(mListener!=null){
                    mListener.onSelected(mDatas.get(position));

                }
            }
        });

    }


    /**
     * 计算屏幕宽度和高度
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);


    }


    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater mInflater;

        private List<FolderBean> mDatas;


        public ListDirAdapter(@NonNull Context context, @NonNull List<FolderBean> objects) {
            super(context, 0, objects);
            mInflater = LayoutInflater.from(context);
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_main,
                        parent, false);
                holder.mImg = convertView.findViewById(R.id.dir_item_image);
                holder.mDirName = convertView.findViewById(R.id.dir_item_name);
                holder.mDirCount = convertView.findViewById(R.id.dir_item_count);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FolderBean bean = getItem(position);
            //重置
            holder.mImg.setImageResource(R.drawable.pictures_no);
            //设置
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO)
                    .loadImage(bean.getFirstImgPath(), holder.mImg);
            holder.mDirName.setText(bean.getName());
            holder.mDirCount.setText(bean.getCount()+"");

            return convertView;


        }

        private class ViewHolder {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}
