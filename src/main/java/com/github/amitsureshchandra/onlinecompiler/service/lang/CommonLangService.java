package com.github.amitsureshchandra.onlinecompiler.service.lang;

import com.github.amitsureshchandra.onlinecompiler.dto.CodeReqDto;
import com.github.amitsureshchandra.onlinecompiler.dto.resp.OutputResp;
import com.github.amitsureshchandra.onlinecompiler.exception.ServerException;
import com.github.amitsureshchandra.onlinecompiler.exception.ValidationException;
import com.github.amitsureshchandra.onlinecompiler.service.docker.DockerService;
import com.github.amitsureshchandra.onlinecompiler.service.file.FileService;
import com.github.amitsureshchandra.onlinecompiler.service.shell.ShellService;
import com.github.amitsureshchandra.onlinecompiler.service.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class CommonLangService implements IContainerRunnerService {
    final DockerService dockerService;
    final FileService fileService;
    final ShellService shellService;

    public CommonLangService(DockerService dockerService, FileService fileService, ShellService shellService) {
        this.dockerService = dockerService;
        this.fileService = fileService;
        this.shellService = shellService;
    }

    @Override
    public OutputResp run(CodeReqDto dto) throws IOException, InterruptedException {
        String userFolder = setUpFiles(dto);

        // creating a container
        String containerName = UUID.randomUUID().toString();
        String command = dockerService.getDockerCommand(userFolder, dto.getCompiler(), containerName);
        log.info("command : " + command);

        // running shell service &
        OutputResp outputResp = shellService.run(command);

        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("start time : " + startTime);
        int waitTime = 10; // 10ms
        Thread.sleep(waitTime);

        // stopping container
        shellService.run("docker stop " + containerName);

        System.out.println("After docker stop "+ LocalDateTime.now());
        // returning output
        outputResp = shellService.run("docker logs " + containerName);

        System.out.println("After docker log "+ LocalDateTime.now());
        System.out.println(outputResp.output().length());
        // clearing docker image
        shellService.run("docker rm " + containerName);

        cleanUp(userFolder);

        return outputResp;
    }

    @Override
    public void cleanUp(String folder) {
        // clearing folder
        FileUtil.deleteFolder(folder);
    }

    @Override
    public String createTempFolder() {
        String userFolder = System.getProperty("user.dir") + "/" + "temp" + File.separator + UUID.randomUUID().toString().substring(0, 6);
        if(!FileUtil.createFolder(userFolder)) {
            log.error("failed to create folder");
            throw new RuntimeException("Server Error");
        }
        return userFolder;
    }

    @Override
    public String setUpFiles(CodeReqDto dto) {
        String userFolder = createTempFolder();
        String filePath = userFolder + "/" + getFileName(dto.getCompiler());
        log.info("filePath : {}", filePath);

        if(!FileUtil.createFile(filePath, dto.getCode())) {
            log.error("failed to write to file for code");
            throw new ServerException("Server Error");
        }

        String inputFilePath = userFolder + "/input.txt";

        if(!FileUtil.createFile(inputFilePath, dto.getInput() == null ? "" : dto.getInput())) {
            log.error("failed to write to file for input");
            throw new ServerException("Server Error");
        }
        return userFolder;
    }

    private String getFileName(String compiler) {
        switch (compiler) {
            case "jdk":
                return "Solution.java";
            case "gcc11":
                return "main.cpp";
            case "node20":
                return "app.js";
            case "golang12":
                return "main.go";
            case "python3":
                return "solution.py";
            default:
                throw new ValidationException("compiler not found");
        }
    }
}
