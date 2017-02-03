package in.letsecho.echoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.HashMap;
import java.util.List;

import in.letsecho.echoapp.library.EntityDisplayModel;

import static android.R.color.white;
import static in.letsecho.echoapp.library.EntityDisplayModel.GROUP_TYPE;

public class PersonAdapterExpandableList extends BaseExpandableListAdapter{

    private Context _context;
    private List<String> _listDataHeader;
    private HashMap<String, List<EntityDisplayModel>> _listDataChild;

    public PersonAdapterExpandableList(Context context, List<String> listDataHeader,
                                       HashMap<String, List<EntityDisplayModel>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        // This disable recycler view and every time a new view is fetched.
        // Recycler view was giving issues because previous item properties were retained
        // if (convertView == null) {
        LayoutInflater infalInflater = (LayoutInflater) this._context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = infalInflater.inflate(R.layout.item_person, parent, false);

        final ImageView photoImageView = (ImageView) convertView.findViewById(R.id.displayImageView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView rightAlignedInfo = (TextView) convertView.findViewById(R.id.rightNumberTextView);

        String sectionHeader = _listDataHeader.get(groupPosition);
        List<EntityDisplayModel> sectionList = _listDataChild.get(sectionHeader);
        EntityDisplayModel entity = sectionList.get(childPosition);
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
            }
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_header_explore, null);
        }
        TextView listHeader = (TextView) convertView.findViewById(R.id.listHeader);
        listHeader.setText(headerTitle);
        // This permanently expands the list
//        ExpandableListView mExpandableListView = (ExpandableListView) parent;
//        mExpandableListView.expandGroup(groupPosition);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
