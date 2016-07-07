/*
 * Copyright 2012 Alexander Lochmann
 *
 * This file is part of SystemTap4Android.
 *
 * SystemTap4Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SystemTap4Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SystemTap4Android.  If not, see <http://www.gnu.org/licenses/>.
 */

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
