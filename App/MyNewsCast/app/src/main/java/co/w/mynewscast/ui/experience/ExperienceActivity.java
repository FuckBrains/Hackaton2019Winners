package co.w.mynewscast.ui.experience;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import co.w.mynewscast.R;
import co.w.mynewscast.model.Article;
import co.w.mynewscast.ui.base.BaseActivity;
import co.w.mynewscast.ui.main.MainActivity;
import co.w.mynewscast.ui.mediaplayer.MediaPlayerActivity;
import co.w.mynewscast.ui.videoplayer.VideoPlayerActivity;
import co.w.mynewscast.utils.TaskDelegate;

public class ExperienceActivity extends BaseActivity implements ExperienceMvpView, TaskDelegate, View.OnClickListener {
    @Inject
    ExperiencePresenter mExperiencePresenter;

    private ExperienceArticleAdapter experienceArticleAdapter;
    private RecyclerView experienceArticleRecyclerView;
    private List<Article> experienceArticleList = new ArrayList<>();


    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setContentView(R.layout.content_experience);

        findViewById(R.id.podcastButton).setOnClickListener(this);
        findViewById(R.id.videoButton).setOnClickListener(this);

        experienceArticleAdapter = new ExperienceArticleAdapter(this, experienceArticleList);
        experienceArticleRecyclerView = findViewById(R.id.experience_list_view);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        experienceArticleRecyclerView.setLayoutManager(mLayoutManager);

        experienceArticleRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(1), true));
        experienceArticleRecyclerView.setItemAnimator(new DefaultItemAnimator());

        experienceArticleRecyclerView.setAdapter(experienceArticleAdapter);

        loadArticles();

        mExperiencePresenter.attachView(this);
    }

    private void loadArticles()
    {
        new JsonTask(this).execute("http://40.76.47.167/api/content/fr");
    }

    @Override
    public void taskCompletionResult(String result) {
        if (result != null) {

            try {
                JSONArray jsonArray = new JSONArray(result);
                int curSize = experienceArticleAdapter.getItemCount();

                for (int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject articleJson = jsonArray.getJSONObject(i);
                    experienceArticleList.add(new Article(articleJson));
                }


                experienceArticleAdapter.notifyItemRangeChanged(curSize, jsonArray.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mExperiencePresenter.detachView();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.podcastButton) {
            startActivity(new Intent(this, MediaPlayerActivity.class));
        } else if (i == R.id.videoButton)
        {
            startActivity(new Intent(this, VideoPlayerActivity.class));
        }
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        private TaskDelegate delegate;

        public JsonTask(TaskDelegate delegate) {
            this.delegate = delegate;
        }

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {


            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            delegate.taskCompletionResult(result);
        }
    }
}
