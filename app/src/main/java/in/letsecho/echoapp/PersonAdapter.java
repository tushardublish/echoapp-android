package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

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

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.displayImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView rightAlignedInfo = (TextView) convertView.findViewById(R.id.rightNumberTextView);

        EntityDisplayModel entity = getItem(position);
        nameTextView.setText(entity.getTitle());
        if (entity.getPhotoUrl() != null) {
            Glide.with(photoImageView.getContext())
                    .load(entity.getPhotoUrl())
                    .into(photoImageView);
        }
        if(entity.getRightAlignedInfo() != null) {
            rightAlignedInfo.setText(entity.getRightAlignedInfo());
            rightAlignedInfo.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}