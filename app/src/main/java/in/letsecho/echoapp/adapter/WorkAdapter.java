package in.letsecho.echoapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import in.letsecho.echoapp.R;
import in.letsecho.echoapp.library.FbWork;
import in.letsecho.echoapp.library.UserDisplayModel;

public class WorkAdapter extends ArrayAdapter<FbWork> {
    public WorkAdapter(Context context, int resource, List<FbWork> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_work, parent, false);
        }

        TextView companyTextView = (TextView) convertView.findViewById(R.id.companyTextView);
        TextView positionTextView = (TextView) convertView.findViewById(R.id.positionTextView);

        FbWork profile = getItem(position);
        companyTextView.setText(profile.getEmployer());
        positionTextView.setText(profile.getPosition());
        return convertView;
    }
}