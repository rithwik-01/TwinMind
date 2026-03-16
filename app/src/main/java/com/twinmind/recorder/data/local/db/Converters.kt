package com.twinmind.recorder.data.local.db

import androidx.room.TypeConverter
import com.twinmind.recorder.data.local.entity.SessionStatus
import com.twinmind.recorder.data.local.entity.SummaryStatus
import com.twinmind.recorder.data.local.entity.UploadStatus

class Converters {
    @TypeConverter fun fromSessionStatus(v: SessionStatus): String   = v.name
    @TypeConverter fun toSessionStatus(v: String): SessionStatus     = SessionStatus.valueOf(v)

    @TypeConverter fun fromUploadStatus(v: UploadStatus): String     = v.name
    @TypeConverter fun toUploadStatus(v: String): UploadStatus       = UploadStatus.valueOf(v)

    @TypeConverter fun fromSummaryStatus(v: SummaryStatus): String   = v.name
    @TypeConverter fun toSummaryStatus(v: String): SummaryStatus     = SummaryStatus.valueOf(v)
}
