package edu.udo.cs.ess.systemtap;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.SystemTapBinder;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class LogFilesOverviewFragment extends SherlockFragment implements OnItemClickListener, OnItemSelectedListener
{
	private static final String TAG = LogFilesOverviewFragment.class.getSimpleName();
	
	private SystemTapService mSystemTapService;
	private ReentrantLock mMutex;
	private LogFileListAdapter mLogFileListAdapter;
	private ListView mListViewModules;
	private int mListViewID;
	private ModuleSpinnerAdapter mModuleSpinnerAdapter;
	private Spinner mSpinnerModules;
	private int mSpinnerModulesID;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.file_list, container, false);
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        mMutex = new ReentrantLock();
		this.getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar);
		
        mLogFileListAdapter = new LogFileListAdapter(this.getActivity());
        mListViewModules = (ListView)this.getActivity().findViewById(R.id.listViewFileList);
		mListViewModules.setEmptyView(this.getActivity().findViewById(android.R.id.empty));
		mListViewModules.setAdapter(mLogFileListAdapter);
		mListViewModules.setOnItemClickListener(this);
		mListViewID = mListViewModules.getId();
		
		mModuleSpinnerAdapter = new ModuleSpinnerAdapter(this.getActivity());
		mSpinnerModules = (Spinner)this.getActivity().findViewById(R.id.spinnerFileList);
		mSpinnerModules.setAdapter(mModuleSpinnerAdapter);
		mSpinnerModules.setOnItemSelectedListener(this);
		mSpinnerModulesID = mSpinnerModules.getId();
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
        	mSystemTapService.unregisterObserver(mModuleSpinnerAdapter);
    		this.getActivity().unbindService(mConnection);
    		Eventlog.d(TAG,"SystemTapService unbounded");
    		mSystemTapService = null;
    	}
    	else
    	{
    		Eventlog.e(TAG, "mSystemTapService is null!");
    	}
    	mMutex.unlock();
    	
    	super.onStop();
    }

	@Override
	public void onItemClick(AdapterView<?> pParent, View pView, int pPosition, long pID)
	{
		if (pParent.getId() == mListViewID)
		{
			File file = (File)mLogFileListAdapter.getItem(pPosition);
			Bundle args = new Bundle();
			/* Pass the selected file as a parameter to the dialog */
			args.putSerializable(SystemTapActivity.LOGFILE_OBJECT, file);
			String name = (String)mSpinnerModules.getSelectedItem();
			args.putString(SystemTapActivity.MODULE_ID, name);
			this.getActivity().showDialog(SystemTapActivity.LOGFILE_DETAILS_DIALOG, args);
		}
		else
		{
			Eventlog.e(TAG,"onClick(): unknown source");
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> pParent, View pView, int pPosition, long pID)
	{
		if (pParent.getId() == mSpinnerModulesID)
		{
			String moduleName = (String)mModuleSpinnerAdapter.getItem(pPosition);
			if (moduleName != null)
			{
				/* Get all logfiles for this module and pass them to the underlying adapter */
				File logFiles[] = mSystemTapService.getLogFiles(moduleName);
				mLogFileListAdapter.setData(logFiles);
			}
			else
			{
				/* The dummy item was selected, no files to display */
				Eventlog.d(TAG,"Dummy item selected");
			}
		}
		else
		{
			Eventlog.e(TAG,"onClick(): unknown source");
		}	
	}

	@Override
	public void onNothingSelected(AdapterView<?> pParent)
	{
		
	}
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	LogFilesOverviewFragment.this.mSystemTapService = ((SystemTapBinder)service).getService();
        	LogFilesOverviewFragment.this.mModuleSpinnerAdapter.setData(mSystemTapService.getModules());
        	Object item = LogFilesOverviewFragment.this.mSpinnerModules.getSelectedItem();
        	if (item != null)
        	{
        		/* Set the ListView to a initial state - if more than 1 module is available, get the selected one and pass the avaible logfiles to the listview-adapter */
        		String moduleName = (String)item;
        		File logFiles[] = LogFilesOverviewFragment.this.mSystemTapService.getLogFiles(moduleName);
        		LogFilesOverviewFragment.this.mLogFileListAdapter.setData(logFiles);
        	}
        	else
        	{
        		Eventlog.e(LogFilesOverviewFragment.TAG,"onServiceConnected(): No item selected");
        	}
        	/* the ModuleSpinnerAdapter wants to get notified if the set of modules has changed */
        	mSystemTapService.registerObserver(LogFilesOverviewFragment.this.mModuleSpinnerAdapter);
        	LogFilesOverviewFragment.this.mMutex.unlock();
    		Eventlog.d(TAG,"SystemTapService bounded");
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };
}
