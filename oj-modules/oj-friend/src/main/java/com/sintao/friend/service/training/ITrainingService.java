package com.sintao.friend.service.training;

import com.sintao.friend.domain.training.TrainingProfile;
import com.sintao.friend.domain.training.dto.TrainingGenerateDTO;
import com.sintao.friend.domain.training.dto.TrainingTaskFinishDTO;
import com.sintao.friend.domain.training.vo.TrainingCurrentVO;

public interface ITrainingService {

    TrainingProfile profile();

    TrainingCurrentVO current();

    TrainingCurrentVO generate(TrainingGenerateDTO trainingGenerateDTO);

    boolean finishTask(TrainingTaskFinishDTO trainingTaskFinishDTO);
}
