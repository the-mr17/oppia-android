package org.oppia.android.app.classroom

import ClassroomListViewModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import org.oppia.android.R
import org.oppia.android.app.drawer.NAVIGATION_PROFILE_ID_ARGUMENT_KEY
import org.oppia.android.app.home.HomeItemViewModel
import org.oppia.android.app.home.WelcomeViewModel
import org.oppia.android.app.home.classroomlist.ClassroomSummaryViewModel
import org.oppia.android.app.home.promotedlist.PromotedStoryListViewModel
import org.oppia.android.app.home.promotedlist.PromotedStoryViewModel
import org.oppia.android.app.home.topiclist.AllTopicsViewModel
import org.oppia.android.app.home.topiclist.TopicSummaryViewModel
import org.oppia.android.app.translation.AppLanguageResourceHandler
import org.oppia.android.app.utility.datetime.DateTimeUtil
import org.oppia.android.databinding.ClassroomListFragmentBinding
import org.oppia.android.domain.classroom.ClassroomController
import org.oppia.android.domain.oppialogger.OppiaLogger
import org.oppia.android.domain.profile.ProfileManagementController
import org.oppia.android.domain.topic.TopicListController
import org.oppia.android.domain.translation.TranslationController
import org.oppia.android.util.parser.html.StoryHtmlParserEntityType
import org.oppia.android.util.parser.html.TopicHtmlParserEntityType
import javax.inject.Inject

class ClassroomListFragmentPresenter @Inject constructor(
  private val activity: AppCompatActivity,
  private val fragment: Fragment,
  private val profileManagementController: ProfileManagementController,
  private val topicListController: TopicListController,
  private val classroomController: ClassroomController,
  private val oppiaLogger: OppiaLogger,
  @TopicHtmlParserEntityType private val topicEntityType: String,
  @StoryHtmlParserEntityType private val storyEntityType: String,
  private val resourceHandler: AppLanguageResourceHandler,
  private val dateTimeUtil: DateTimeUtil,
  private val translationController: TranslationController,
) {
  private lateinit var binding: ClassroomListFragmentBinding
  private var internalProfileId: Int = -1

  fun handleCreateView(inflater: LayoutInflater, container: ViewGroup?): View? {
    binding = ClassroomListFragmentBinding.inflate(
      inflater,
      container,
      /* attachToRoot= */ false
    )

    internalProfileId = activity.intent.getIntExtra(NAVIGATION_PROFILE_ID_ARGUMENT_KEY, -1)

    val classroomListViewModel = ClassroomListViewModel(
      activity,
      fragment,
      oppiaLogger,
      internalProfileId,
      profileManagementController,
      topicListController,
      classroomController,
      topicEntityType,
      storyEntityType,
      resourceHandler,
      dateTimeUtil,
      translationController
    )

    classroomListViewModel.homeItemViewModelListLiveData.observe(activity) {
      binding.composeView.apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
          MaterialTheme {
            ClassroomListScreen(it)
          }
        }
      }
    }

    return binding.root
  }

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  fun ClassroomListScreen(homeItemViewModelList: List<HomeItemViewModel>) {
    val groupedItems = homeItemViewModelList.groupBy { it::class }
    LazyColumn {
      groupedItems.forEach { (type, items) ->
        when (type) {
          WelcomeViewModel::class -> items.forEach { item ->
            item {
              WelcomeComponent(welcomeViewModel = item as WelcomeViewModel)
            }
          }
          PromotedStoryListViewModel::class -> items.forEach { item ->
            item {
              PromotedStoryListComponent(promotedStoryListViewModel = item as PromotedStoryListViewModel)
            }
          }
          ClassroomSummaryViewModel::class -> stickyHeader {
            ClassroomListComponent(classroomSummaryList = items.map { it as ClassroomSummaryViewModel })
          }
          AllTopicsViewModel::class -> items.forEach { item ->
            item {
              AllTopicsHeaderComponent(allTopicsViewModel = item as AllTopicsViewModel)
            }
          }
          TopicSummaryViewModel::class -> item {
            TopicListComponent(topicSummaryList = items.map { it as TopicSummaryViewModel })
          }
        }
      }
    }
  }

  @Composable
  fun WelcomeComponent(welcomeViewModel: WelcomeViewModel) {
    val outerPadding = dimensionResource(id = R.dimen.home_welcome_outer_padding)
    val textMarginEnd = dimensionResource(id = R.dimen.home_welcome_text_view_margin_end)
    val greetingLineColor = colorResource(
      id = R.color.component_color_home_activity_layout_greeting_text_line_color
    )

    Text(
      text = welcomeViewModel.computeWelcomeText(),
      modifier = Modifier
        .padding(
          start = outerPadding,
          top = outerPadding,
          end = outerPadding + textMarginEnd,
        )
        .drawBehind {
          val strokeWidthPx = 6.dp.toPx()
          val verticalOffset = size.height + 4.dp.toPx()
          drawLine(
            color = greetingLineColor,
            strokeWidth = strokeWidthPx,
            start = Offset(x = 0f, y = verticalOffset),
            end = Offset(x = size.width, y = verticalOffset),
          )
        },
      color = colorResource(id = R.color.component_color_shared_primary_text_color),
      fontSize = 24.sp,
      fontFamily = FontFamily.SansSerif,
    )
  }

  @Composable
  fun PromotedStoryListComponent(promotedStoryListViewModel: PromotedStoryListViewModel) {
    Text(
      text = promotedStoryListViewModel.getHeader(),
      color = colorResource(id = R.color.component_color_shared_primary_text_color),
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 18.sp,
      modifier = Modifier
        .padding(
          start = dimensionResource(id = R.dimen.classrooms_text_margin_start),
          top = dimensionResource(id = R.dimen.classrooms_text_margin_top),
          end = dimensionResource(id = R.dimen.classrooms_text_margin_end),
          bottom = dimensionResource(id = R.dimen.classrooms_text_margin_bottom),
        )
    )
    LazyRow(
      modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
      items(promotedStoryListViewModel.promotedStoryList) {
        PromotedStoryCard(promotedStoryViewModel = it)
      }
    }
  }

  @Composable
  fun ClassroomListComponent(classroomSummaryList: List<ClassroomSummaryViewModel>) {
    Text(
      text = stringResource(id = R.string.classrooms),
      color = colorResource(id = R.color.component_color_shared_primary_text_color),
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 18.sp,
      modifier = Modifier
        .padding(
          start = dimensionResource(id = R.dimen.classrooms_text_margin_start),
          top = dimensionResource(id = R.dimen.classrooms_text_margin_top),
          end = dimensionResource(id = R.dimen.classrooms_text_margin_end),
          bottom = dimensionResource(id = R.dimen.classrooms_text_margin_bottom),
        )
    )
    LazyRow(
      modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
      items(classroomSummaryList) {
        ClassroomCard(classroomSummaryViewModel = it)
      }
    }
  }

  @Composable
  fun TopicListComponent(topicSummaryList: List<TopicSummaryViewModel>) {
    Text(text = "TopicList")
  }

  @Composable
  fun AllTopicsHeaderComponent(allTopicsViewModel: AllTopicsViewModel) {
    Text(text = "AllTopics")
  }

  @Composable
  fun ClassroomCard(classroomSummaryViewModel: ClassroomSummaryViewModel) {
    Card(
      modifier = Modifier
        .height(182.dp)
        .width(150.dp)
        .padding(
          start = dimensionResource(R.dimen.promoted_story_card_layout_margin_start),
          end = dimensionResource(R.dimen.promoted_story_card_layout_margin_end),
        ),
      border = BorderStroke(2.dp, color = colorResource(id = R.color.color_def_oppia_green)),
      elevation = 4.dp,
    ) {
      Column(
        modifier = Modifier.padding(all = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = classroomSummaryViewModel.thumbnailResourceId),
          contentDescription = "${classroomSummaryViewModel.title} Card",
          modifier = Modifier
            .padding(bottom = 20.dp)
            .size(80.dp),
        )
        Text(
          text = classroomSummaryViewModel.title,
          color = colorResource(id = R.color.component_color_shared_primary_text_color),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
        )
      }
    }
  }

  @Composable
  fun PromotedStoryCard(promotedStoryViewModel: PromotedStoryViewModel) {
    Card(
      modifier = Modifier
        .width(280.dp)
        .padding(
          start = dimensionResource(R.dimen.promoted_story_card_layout_margin_start),
          end = dimensionResource(R.dimen.promoted_story_card_layout_margin_end),
          bottom = 8.dp,
        ),
      elevation = 4.dp,
    ) {
      Column(
        verticalArrangement = Arrangement.Center,
      ) {
        Image(
          painter = painterResource(id = promotedStoryViewModel.thumbnailResourceId),
          contentDescription = "${promotedStoryViewModel.storyTitle} Card",
          modifier = Modifier
            .aspectRatio(16f / 9f)
            .background(
              Color(
                (0xff000000L or promotedStoryViewModel.promotedStory.lessonThumbnail
                  .backgroundColorRgb.toLong()).toInt()
              )
            )
        )
        Text(
          text = promotedStoryViewModel.nextChapterTitle,
          modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
          color = colorResource(id = R.color.component_color_shared_primary_text_color),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
          textAlign = TextAlign.Start,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        /*Text(
          text = promotedStoryViewModel.storyTitle,
          modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp),
          color = colorResource(id = R.color.component_color_shared_primary_text_color),
          fontFamily = FontFamily.SansSerif,
          fontSize = 16.sp,
          textAlign = TextAlign.Start,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )*/
        Text(
          text = promotedStoryViewModel.topicTitle.uppercase(),
          modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
          color = colorResource(id = R.color.component_color_shared_story_card_topic_name_text_color),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Light,
          fontSize = 14.sp,
          textAlign = TextAlign.Start,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = promotedStoryViewModel.classroomTitle.uppercase(),
          modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
            .border(
              width = 2.dp,
              color = colorResource(id = R.color.color_def_persian_blue),
              shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
          color = colorResource(id = R.color.color_def_persian_blue),
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          textAlign = TextAlign.Start,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
