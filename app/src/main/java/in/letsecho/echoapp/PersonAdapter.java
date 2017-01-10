package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.twitter.sdk.android.core.models.User;

import java.util.List;

import in.letsecho.library.UserDisplayModel;
import in.letsecho.library.UserProfile;

public class PersonAdapter extends ArrayAdapter<UserDisplayModel> {
    public PersonAdapter(Context context, int resource, List<UserDisplayModel> objects) {
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

        UserDisplayModel person = getItem(position);
        nameTextView.setText(person.getName());
        if (person.getPhotoUrl() != null) {
            Glide.with(photoImageView.getContext())
                    .load(person.getPhotoUrl())
                    .into(photoImageView);
        }
        if(person.getRightAlignedInfo() != null) {
            rightAlignedInfo.setText(person.getRightAlignedInfo());
            rightAlignedInfo.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
