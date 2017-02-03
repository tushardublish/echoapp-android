package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.List;

import in.letsecho.echoapp.library.EntityDisplayModel;

public class PersonAdapter extends ArrayAdapter<EntityDisplayModel> {
    public PersonAdapter(Context context, int resource, List<EntityDisplayModel> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_person, parent, false);
        }

        final ImageView photoImageView = (ImageView) convertView.findViewById(R.id.displayImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView rightAlignedInfo = (TextView) convertView.findViewById(R.id.rightNumberTextView);

        EntityDisplayModel entity = getItem(position);
        nameTextView.setText(entity.getTitle());
        if (entity.getPhotoUrl() != null) {
            // Load rounded photo
            Glide.with(photoImageView.getContext()).load(entity.getPhotoUrl()).asBitmap().centerCrop()
                .into(new BitmapImageViewTarget(photoImageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(photoImageView.getContext().getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        photoImageView.setImageDrawable(circularBitmapDrawable);
                    }
            });
        }
        if(entity.getRightAlignedInfo() != null) {
            rightAlignedInfo.setText(entity.getRightAlignedInfo());
            rightAlignedInfo.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}