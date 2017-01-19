package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import in.letsecho.library.ChatMessage;

public class MessageAdapter extends ArrayAdapter<ChatMessage> {
    private String userId;

    public MessageAdapter(Context context, int resource, List<ChatMessage> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);

        ChatMessage message = getItem(position);
        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            messageTextView.setVisibility(View.GONE);
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .into(photoImageView);
        } else {
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }
        authorTextView.setVisibility(View.VISIBLE);
        authorTextView.setText(message.getName());

        if(message.getSenderUid()!=null && message.getSenderUid().equals(userId)) {
            messageTextView.setGravity(Gravity.END);
            authorTextView.setGravity(Gravity.END);
        } else {
            messageTextView.setGravity(Gravity.START);
            authorTextView.setGravity(Gravity.START);
        }
        return convertView;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
