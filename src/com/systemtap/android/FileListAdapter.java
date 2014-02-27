package com.systemtap.android;
import java.io.File;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Databackend for the ListView showing all log files for a specific module
 * It gets updated when an user selects a module from the spinner.
 * @author alex
 *
 */
public class FileListAdapter extends ArrayAdapter<File>
{
	private static final String TAG = FileListAdapter.class.getSimpleName();
	
	public FileListAdapter(Context pContext,int pResource) {
		super(pContext,pResource);
	}
	
	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
		View itemView = super.getView(pPosition,pConvertView,pParent);
		
		TextView textViewLogFileListName = (TextView)itemView.findViewById(R.id.textViewFileListName);		
		File file = this.getItem(pPosition);
		/* Get file name */
		String fileName = file.getName();
		/* Strip off the extension */
		fileName = fileName.substring(0,fileName.lastIndexOf('.'));
		int dateEndPos = fileName.lastIndexOf('_') - 1;
		/* extract the date */
		String date = fileName.substring(fileName.lastIndexOf('_',dateEndPos) + 1,dateEndPos + 1);
		/* extract the time */
		String time = fileName.substring(fileName.lastIndexOf('_') + 1);
		time = time.replace(".", ":");
		textViewLogFileListName.setText(date + " " + time);
		
		return itemView;
	}
}
