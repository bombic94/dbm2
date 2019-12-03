package cz.zcu.kiv.dbm2.sp.controller;

import cz.zcu.kiv.dbm2.sp.dto.RdfTypesSelectionDto;
import cz.zcu.kiv.dbm2.sp.model.RdfFormat;
import cz.zcu.kiv.dbm2.sp.service.RdfService;
import org.apache.jena.ext.com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;


@Controller
public class RdfController {

    @Autowired
    RdfService rdfService;

    @GetMapping("/")
    public String showUploadForm() {
        return "uploadForm";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        rdfService.generateTypes(file);
        return "redirect:/rename";
    }

    @GetMapping("/rename")
    public String showRenameForm(Model model, @RequestParam(value = "error" , required = false) String error) {
        RdfTypesSelectionDto dto = new RdfTypesSelectionDto();
        dto.setTypes(rdfService.getRdfTypes());
        model.addAttribute("dto", dto);
        model.addAttribute("error", error);
        return "renameModelForm";
    }

    @PostMapping("/rename")
    public String chooseTypes(@ModelAttribute("dto") RdfTypesSelectionDto dto,
                              @RequestParam(value = "includeOrigId" , required = false) String[] includeOrigId,
                              @RequestParam(value = "selectedProperties" , required = false) String[] selectedProperties) {
        rdfService.renameModel(includeOrigId, selectedProperties);
        return "redirect:/confirm";
    }

    @GetMapping("/confirm")
    public String confirm(Model model) {
        model.addAttribute("renamingMap", rdfService.getRenamingMap());
        return "confirmForm";
    }

    @PostMapping(value="/confirm", params = "cancel")
    public String cancelConfirm() {
        return "redirect:/rename";
    }

    @PostMapping(value="/confirm", params = "save")
    public String saveConfirm() {
        return "redirect:/download";
    }

    @PostMapping(value="/confirm", params = "changes")
    public String changesConfirm() {
        return "redirect:/changes";
    }

    @GetMapping("/download")
    public void getFile(HttpServletResponse response) {
        try {
            // get your file as InputStream
            org.apache.jena.rdf.model.Model renamedModel = rdfService.getRenamedModel();
            RdfFormat rdfFormat = rdfService.getRdfFormat();

            response.setContentType(rdfFormat.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"renamed." + rdfFormat.getExtension() + "\"");
            renamedModel.write(response.getOutputStream(), rdfFormat.getLang());
            response.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("IOError writing file to output stream");
        }

    }

    @GetMapping("/changes")
    public void getChanges(HttpServletResponse response) {
        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=changes.csv");
            //head line
            response.getWriter().append("New URI;Num of instances;Old URI").append(System.getProperty("line.separator"));
            //each change. Values separated by semicolon, items in list by comma
            for (Map.Entry<String, List<String>> entry : rdfService.getRenamingMap().entrySet()) {
                response.getWriter().append(entry.getKey())
                        .append(';')
                        .append(entry.getValue().size() + "")
                        .append(';');
                for (String s : entry.getValue()) {
                    response.getWriter().append(s)
                            .append(',');
                }
                response.getWriter().append(System.getProperty("line.separator"));
            }
            response.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("IOError writing file to output stream");
        }

    }
}
