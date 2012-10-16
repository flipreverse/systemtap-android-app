package edu.udo.cs.ess.systemtap;

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
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.SystemTapBinder;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class ModulesOverviewFragment extends SherlockFragment implements OnItemClickListener
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
		this.getActivity().setTheme(com.actionbarsherlock.R.style.Theme_Sherlock_Light_DarkActionBar);
        mModuleListAdapter = new ModuleListAdapter(this.getActivity());
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
    	mMutex.lock();
    	if (mSystemTapService != null)
    	{
        	mSystemTapService.registerObserver(mModuleListAdapter);
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
			String moduleName = (String)mModuleListAdapter.getItem(pPosition);
			Bundle args = new Bundle();
			args.putString(SystemTapActivity.MODULE_ID_NAME, moduleName);
			this.getActivity().showDialog(SystemTapActivity.MODULE_DETAILS_DIALOG, args);
		}
		else
		{
			Eventlog.e(TAG,"onClick(): unknown source");
		}
	}
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mSystemTapService = ((SystemTapBinder)service).getService();
        	ModulesOverviewFragment.this.mModuleListAdapter.setData(mSystemTapService.getModules());
        	mSystemTapService.registerObserver(ModulesOverviewFragment.this.mModuleListAdapter);
        	ModulesOverviewFragment.this.mMutex.unlock();
    		Eventlog.d(TAG,"SystemTapService bounded");
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };
}
