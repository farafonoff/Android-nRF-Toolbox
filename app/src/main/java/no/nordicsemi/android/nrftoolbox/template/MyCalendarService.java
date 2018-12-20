package no.nordicsemi.android.nrftoolbox.template;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nordicsemi.android.nrftoolbox.template.calendarservice.CalendarEvent;
import no.nordicsemi.android.nrftoolbox.template.calendarservice.CalendarService;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MyCalendarService extends IntentService {
    private static final String ACTION_INIT_ALARM = "no.nordicsemi.android.nrftoolbox.template.action.ACTION_INIT_ALARM";
    private static final String ACTION_RECEIVE_ALARM = "no.nordicsemi.android.nrftoolbox.template.action.ACTION_RECEIVE_ALARM";

    /*// TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "no.nordicsemi.android.nrftoolbox.template.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "no.nordicsemi.android.nrftoolbox.template.extra.PARAM2";*/

    public MyCalendarService() {
        super("MyCalendarService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionInit(Context context) {
        Intent intent = new Intent(context, MyCalendarService.class);
        intent.setAction(ACTION_INIT_ALARM);
        context.startService(intent);
    }

    public static void startActionAlarm(Context context) {
        Intent intent = new Intent(context, MyCalendarService.class);
        intent.setAction(ACTION_RECEIVE_ALARM);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_INIT_ALARM.equals(action)) {
                initAlarmSystem();
            } else if (ACTION_RECEIVE_ALARM.equals(action)) {
                onAlarm();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void initAlarmSystem() {
        Date cdate = new Date();
        cdate.setTime(cdate.getTime() + 65000);
        setAlarm(cdate);
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void onAlarm() {
        List<CalendarEvent> events = readEvents();
        Date now = new Date();
        Date next15 = new Date(now.getTime()+15*60*1000);
        CalendarEvent current = getNextEvent(events);
        if (current != null) {
            TemplateService.startForMessage(this, current.getTitle());
            if (next15.after(current.getBegin())) {
                CalendarEvent next = getNextEvent(events, current);
                if (next != null) {
                    setAlarm(next.getBegin());
                    return;
                }
            } else {
                setAlarm(current.getBegin());
                return;
            }
        }
        Date cdate = new Date();
        cdate.setTime(cdate.getTime() + 24 * 60 * 60 * 1000);
        setAlarm(cdate);
    }

    private void setAlarm(Date date) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 234324243, intent, 0);
        AlarmManager manager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
        Date adate = new Date(date.getTime()-120*1000);
        manager.setExactAndAllowWhileIdle( AlarmManager.RTC_WAKEUP, adate.getTime(), pendingIntent );
        Log.i("MyCalendar", "Next alarm at "+ adate);
    }

    private CalendarEvent getNextEvent(List<CalendarEvent> events) {
        Date current = new Date();
        Collections.sort(events, Comparator.comparing(CalendarEvent::getBegin));
        for(CalendarEvent event: events) {
            if (event.getBegin().after(current)) {
                return event;
            }
        }
        return null;
    }

    private CalendarEvent getNextEvent(List<CalendarEvent> events, CalendarEvent next) {
        Collections.sort(events, Comparator.comparing(CalendarEvent::getBegin));
        for(CalendarEvent event: events) {
            if (event!=next && event.getBegin().after(next.getBegin())) {
                return event;
            }
        }
        return null;
    }

    private List<CalendarEvent> readEvents() {
        HashMap<String, List<CalendarEvent>> events = CalendarService.readCalendar(this);
        List<CalendarEvent> result = new ArrayList<>();
        for(Map.Entry<String, List<CalendarEvent>> entry: events.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }
}
