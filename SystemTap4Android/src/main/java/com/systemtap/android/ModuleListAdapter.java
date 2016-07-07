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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.systemtap.android.service.Module;


public class ModuleListAdapter extends ArrayAdapter<Module> {
	private static final String TAG = ModuleListAdapter.class.getSimpleName();

	
	public ModuleListAdapter(Context pContext, int pResourceLayout, int pResourceTextView) {
		super(pContext,pResourceLayout,pResourceTextView);
	}
	
	@Override
	public View getView(int pPosition, View pConvertView, ViewGroup pParent) {
		View itemView = super.getView(pPosition,pConvertView,pParent);
		
		Module module = this.getItem(pPosition);
		if (module == null)
		{
			Log.e(TAG,"caller requests unknown item: " + pPosition);
			return null;
		}
		
		TextView textViewModulename = (TextView)itemView.findViewById(R.id.textViewModuleName);
		textViewModulename.setText(module.getName());
		
		TextView textViewModuleStatus = (TextView)itemView.findViewById(R.id.textViewModuleStatus);
		/* Depending on a modules status set the displayed text and its color */
		switch(module.getStatus())
		{
			case RUNNING:
				textViewModuleStatus.setText(this.getContext().getText(R.string.stap_module_running));
				textViewModuleStatus.setTextColor(this.getContext().getResources().getColor(R.color.stap_module_running));
				break;

			case STOPPED:
				textViewModuleStatus.setText(this.getContext().getText(R.string.stap_module_stopped));
				textViewModuleStatus.setTextColor(this.getContext().getResources().getColor(R.color.stap_module_stopped));
				break;

			case CRASHED:
				textViewModuleStatus.setText(this.getContext().getText(R.string.stap_module_crashed));
				textViewModuleStatus.setTextColor(this.getContext().getResources().getColor(R.color.stap_module_crashed));
				break;
		}
		
		return itemView;
	}	
}
