package cn.easydone.androidfluxpractice.ui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import cn.easydone.androidfluxpractice.R;
import cn.easydone.androidfluxpractice.RxBus;
import cn.easydone.androidfluxpractice.action.UserActionCreator;
import cn.easydone.androidfluxpractice.bean.User;
import cn.easydone.androidfluxpractice.dispatcher.Dispatcher;
import cn.easydone.androidfluxpractice.store.UserStore;
import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @ViewById(R.id.toolbar)
    Toolbar toolbar;
    @ViewById(R.id.fab)
    FloatingActionButton fab;

    private UserActionCreator userActionCreator;
    private UserStore userStore;
    private UserAdapter userAdapter;
    private Dispatcher dispatcher;
    private List<User> userList;
    private CircularProgressBar progressBar;
    private CompositeSubscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDependencies();
    }

    private void initDependencies() {
        userStore = UserStore.getInstance();
        subscription = new CompositeSubscription();
        dispatcher = Dispatcher.getInstance();
        dispatcher.register(userStore);
        userActionCreator = UserActionCreator.getInstance(dispatcher);
    }

    @Click(R.id.fab)
    void fabClick() {
        Snackbar.make(fab, ToastSth.toast(), Snackbar.LENGTH_SHORT).show();
    }

    @AfterViews
    void setupView() {
        setSupportActionBar(toolbar);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        progressBar = (CircularProgressBar) findViewById(R.id.progress_bar);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        assert recyclerView != null;
        recyclerView.setLayoutManager(linearLayoutManager);

        userList = userStore.getUserList();
        userAdapter = new UserAdapter(userList, MainActivity.this);
        recyclerView.setAdapter(userAdapter);

        List<String> users = new ArrayList<>();
        users.add("liangzhitao");
        users.add("AlanCheen");
        users.add("yongjhih");
        users.add("zzz40500");
        users.add("greenrobot");
        users.add(ToastSth.people());

        userActionCreator.fetchData(users);

        onEvent();
    }

    private void onEvent() {
        subscription.add(toSubscription(UserStore.LoadingStartChangeEvent.class,
                new Action1<UserStore.LoadingStartChangeEvent>() {
                    @Override
                    public void call(UserStore.LoadingStartChangeEvent changeEvent) {
                        if (changeEvent != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }));

        subscription.add(toSubscription(UserStore.InitRecyclerViewChangeEvent.class,
                new Action1<UserStore.InitRecyclerViewChangeEvent>() {
                    @Override
                    public void call(UserStore.InitRecyclerViewChangeEvent changeEvent) {
                        progressBar.setVisibility(View.GONE);
                        if (changeEvent != null) {
                            userList = userStore.getUserList();
                            userAdapter.refreshUi(userList);
                        }
                    }
                }));

        subscription.add(toSubscription(UserStore.ErrorChangeEvent.class,
                new Action1<UserStore.ErrorChangeEvent>() {
                    @Override
                    public void call(UserStore.ErrorChangeEvent changeEvent) {
                        progressBar.setVisibility(View.GONE);
                        if (changeEvent != null) {
                            Throwable throwable = userStore.getThrowable();
                            Toast.makeText(MainActivity.this, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }));
    }

    private <T> Subscription toSubscription(Class<T> type, Action1<T> action1) {
        return RxBus.getInstance().toSubscription(type, action1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispatcher.unRegister(userStore);
        subscription.unsubscribe();
    }
}
