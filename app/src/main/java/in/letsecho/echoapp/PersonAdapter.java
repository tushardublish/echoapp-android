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

import in.letsecho.library.UserProfile;

public class PersonAdapter extends ArrayAdapter<UserProfile> {
    public PersonAdapter(Context context, int resource, List<UserProfile> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_person, parent, false);
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.displayImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        UserProfile person = getItem(position);
        nameTextView.setText(person.getName());
        if (person.getPhotoUrl() != null) {
            Glide.with(photoImageView.getContext())
                    .load(person.getPhotoUrl())
                    .into(photoImageView);
        }
        return convertView;
    }
}
