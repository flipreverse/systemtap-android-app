package edu.udo.cs.ess.systemtap;

import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import edu.udo.cs.ess.logging.Eventlog;
import edu.udo.cs.ess.systemtap.service.SystemTapBinder;
import edu.udo.cs.ess.systemtap.service.SystemTapService;

public class SystemtapActivity extends Activity implements OnClickListener 
{
	private static final String TAG = SystemtapActivity.class.getSimpleName();
	
	private SystemTapService mSystemTapService;
	private ReentrantLock mMutex;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_systemtap);
        ((Button)this.findViewById(R.id.button1)).setOnClickListener(this);
        ((Button)this.findViewById(R.id.button2)).setOnClickListener(this);
        mMutex = new ReentrantLock();
    }

    @Override
    public void onStart()
    {
    	Intent intent = new Intent(this,SystemTapService.class);
    	this.startService(intent);
    	
    	mMutex.lock();
    	this.bindService(new Intent(this,SystemTapService.class), mConnection, 0);
    	
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
    		this.unbindService(mConnection);
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_systemtap, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.button1)
		{
			if (mSystemTapService != null)
			{
				mSystemTapService.startModule("foo");
			}
		}
		else
		{

			if (mSystemTapService != null)
			{
				mSystemTapService.stopModule("foo");
			}
		}
	}

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mSystemTapService = ((SystemTapBinder)service).getService();
        	SystemtapActivity.this.mMutex.unlock();
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSystemTapService = null;
        }
    };
}
