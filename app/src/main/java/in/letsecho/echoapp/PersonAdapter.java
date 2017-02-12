package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
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

import static android.R.color.white;
import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;

public class PersonAdapter extends ArrayAdapter<EntityDisplayModel> {

    private Context _context;

    public PersonAdapter(Context context, int resource, List<EntityDisplayModel> objects) {
        super(context, resource, objects);
        this._context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // This disable recycler view and every time a new view is fetched.
        // Recycler view was giving issues because previous item properties were retained
        // if (convertView == null) {
        convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_person, parent, false);

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
        // Set right aligned info
        if(entity.getRightAlignedInfo() != null) {
            rightAlignedInfo.setText(entity.getRightAlignedInfo());
            rightAlignedInfo.setVisibility(View.VISIBLE);
            if(entity.getType() == GROUP_TYPE && entity.getRightAlignedInfo()
                    .equals(_context.getString(R.string.group_type_service))) {
                rightAlignedInfo.setBackgroundResource(R.drawable.bg_light_blue);
                rightAlignedInfo.setTextColor(ContextCompat.getColor(_context, white));
            } else if(entity.getType() == GROUP_TYPE && entity.getRightAlignedInfo()
                    .equals(_context.getString(R.string.group_type_group))) {
                rightAlignedInfo.setBackgroundResource(R.drawable.bg_light_green);
                rightAlignedInfo.setTextColor(ContextCompat.getColor(_context, white));
            } else {
                rightAlignedInfo.setBackgroundResource(R.drawable.bg_light_grey);
                rightAlignedInfo.setTextColor(ContextCompat.getColor(_context, white));
            }

        }

        return convertView;
    }
}