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

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.systemtap.android.service.Module;
import com.systemtap.android.service.SystemTapBinder;
import com.systemtap.android.service.SystemTapService;

public class ModulesOverviewFragment extends SherlockFragment implements OnItemClickListener, Observer
{
	private static final String TAG = ModulesOverviewFragment.class.getSimpleName();
		
	private SystemTapService mSystemTapService;
	private ReentrantLock mMutex;
	private ModuleListAdapter mModuleListAdapter;
	private ListView mListViewModules;
	private int mListViewID;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.module_list, container, false);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        mMutex = new ReentrantLock();
		this.getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
        mModuleListAdapter = new ModuleListAdapter(this.getSherlockActivity().getSupportActionBar().getThemedContext(),R.layout.module_list_item,R.id.textViewModuleName);
        mListViewModules = (ListView)this.getActivity().findViewById(R.id.listViewModules);
		mListViewModules.setEmptyView(this.getActivity().findViewById(android.R.id.empty));
		mListViewModules.setAdapter(mModuleListAdapter);
		mListViewModules.setOnItemClickListener(this);
		mListViewID = mListViewModules.getId();
	}
	
    @Override
    public void onStart()
    {
    	Intent intent = new Intent(this.getActivity(),SystemTapService.class);
    	this.getActivity().startService(intent);

    	/* Hold the lock, so onStop() will block til the service is bounded */
    	mMutex.lock();
    	this.getActivity().bindService(new Intent(this.getActivity(),SystemTapService.class), mConnection, 0);
    	
    	super.onStart();
    }

    @Override
    public void onResume()
    {
    	super.onResume();
    }

    @Override
    public void onPause()
    {
    	super.onPause();
    }

    @Override
    public void onStop()
    {
    	/* If the application immediately gets suspend, wait until the service is bounded */
    	mMutex.lock();
    	if (mSystemTapService != null)
    	{
        	mSystemTapService.unregisterObserver(this);
    		Log.d(TAG,"SystemTapService unbounded");
    		mSystemTapService = null;
    	}
    	else
    	{
    		Log.e(TAG, "mSystemTapService is null!");
    	}
		this.getActivity().unbindService(mConnection);
    	mMutex.unlock();
    	
    	super.onStop();
    }

	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pID)
	{
		if (pParent.getId() == mListViewID)
		{
			String moduleName = mModuleListAdapter.getItem(pPosition).getName();
			Bundle args = new Bundle();
			/* Pass the selected module as a parameter to the dialog */
			args.putString(SystemTapActivity.MODULE_ID, moduleName);
			this.getActivity().showDialog(SystemTapActivity.MODULE_DETAILS_DIALOG, args);
		}
		else
		{
			Log.e(TAG,"onClick(): unknown source");
		}
	}
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mSystemTapService = ((SystemTapBinder)service).getService();
        	/* Init the ModuleListAdapter */
        	ModulesOverviewFragment.this.refreshModuleList(mSystemTapService.getModules());
        	/* the ModuleListAdapter wants to get notified if the set of modules has changed */
        	mSystemTapService.registerObserver(ModulesOverviewFragment.this);
        	ModulesOverviewFragment.this.mMutex.unlock();
    		Log.d(TAG,"SystemTapService bounded");
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };

	@Override
	public void update(Observable pObservable, Object pData) {
		/* Some sanity checks to ensure pData is a Collection */
		if (pData instanceof Collection<?>) {
			this.refreshModuleList((Collection<Module>)pData);
		}
	}
	
	private void refreshModuleList(Collection<Module> pModules) {
		final Collection<Module> modules = pModules;
		/* If the adapter gets new data, a call to notifyDataSetChanged() should be done from the ui thread. */
		/* Only the ui thread is allowed to manipulate the gui */
		FragmentActivity activity = this.getActivity();
		if (activity == null) {
			return;
		}

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mModuleListAdapter.clear();
				if(android.os.Build.VERSION.SDK_INT < 11) {
		        	for (Module module : modules) {
		        		mModuleListAdapter.add(module);
		        	}
				} else {
					mModuleListAdapter.addAll(modules);
				}
			}
		});
	}
}
