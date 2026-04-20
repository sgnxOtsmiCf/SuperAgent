package cn.sgnxotsmicf.chatMemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/20 14:55
 * @Version: 1.0
 * @Description:
 */
@Component
@Slf4j
public class FileBasedMemory implements ChatMemory {

    private final String BASE_DIR;
    private static final Kryo kryo = new Kryo();
    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }
    //构造对象时指定文件保存目录
    public FileBasedMemory(String baseDir) {
        BASE_DIR = baseDir;
        File dir = new File(BASE_DIR);
        if (!dir.exists()) {
            boolean flag = dir.mkdirs();
            log.info("Create directory status: " + flag);
        }
    }


    @Override
    public void add(@NotNull String conversationId, @NotNull Message message) {
        List<Message> conversationMessage = getOrCreateConversation(conversationId);
        conversationMessage.add(message);
        saveConversation(conversationId,conversationMessage);
    }


    @Override
    public void add(@NotNull String conversationId, @NotNull List<Message> messages) {
        List<Message> conversationMessage = getOrCreateConversation(conversationId);
        conversationMessage.addAll(messages);
        saveConversation(conversationId,conversationMessage);
    }

    /**
     * 取出全部数据
     * @param conversationId 对话id
     * @return Message的List数组
     */
    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        return getOrCreateConversation(conversationId);
    }


    public List<Message> getLast(String conversationId, int LastNumber) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList
                .stream()
                .skip(Math.max(0, messageList.size() - LastNumber))
                .limit(messageList.size())
                .collect(Collectors.toList());
    }

    @Override
    public void clear(@NotNull String conversationId) {
        File conversationFile = getConversationFile(conversationId);
        if (conversationFile.exists()) {
            conversationFile.delete();
        }
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File conversationFile = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(conversationFile))) {
            kryo.writeObject(output, messages);
        }catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private List<Message> getOrCreateConversation(String conversationId) {
        File conversationFile = getConversationFile(conversationId);
        ArrayList<Message> messages = new ArrayList<>();
        if (conversationFile.exists()) {
            try(Input input = new Input(new FileInputStream(conversationFile))) {
                messages = kryo.readObject(input, ArrayList.class);
            }catch (Exception e) {
                log.error("Error reading conversation file: " + conversationFile.getPath(), e);
            }
        }
        return messages;
    }

    public File getConversationFile(String conversationId) {
        return new File(BASE_DIR,conversationId + ".kryo");
    }

}
