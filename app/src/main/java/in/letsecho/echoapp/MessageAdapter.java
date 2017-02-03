package in.letsecho.echoapp;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import in.letsecho.library.ChatMessage;

import static in.letsecho.echoapp.R.drawable.text_bg_grey;

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
        LinearLayout messageLayout = (LinearLayout)convertView.findViewById(R.id.messageLinearLayout);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)messageLayout.getLayoutParams();
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);

        ChatMessage message = getItem(position);
        boolean isPhoto = message.getPhotoUrl() != null;
        //Set Photo
        if (isPhoto) {
            photoImageView.setVisibility(View.VISIBLE);
            messageLayout.setVisibility(View.GONE);
            Glide.with(photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .into(photoImageView);
        } else {
            photoImageView.setVisibility(View.GONE);
            messageLayout.setVisibility(View.VISIBLE);
            messageTextView.setText(message.getText());
        }
        //Set Alignment
        authorTextView.setText(message.getName());
        if(message.getSenderUid()!=null && message.getSenderUid().equals(userId)) {
            messageLayout.setBackgroundResource(R.drawable.text_bg_grey);
            layoutParams.gravity = Gravity.END;
            authorTextView.setGravity(Gravity.END);
        } else {
            messageLayout.setBackgroundResource(R.drawable.text_bg_blue);
            layoutParams.gravity = Gravity.START;
            authorTextView.setGravity(Gravity.START);
        }
        //setBackgroundResource resets padding, so this statement is required to apply gravity
        messageLayout.setPadding(10,10,10,10);
        messageLayout.setGravity(Gravity.CENTER_VERTICAL);

        return convertView;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
