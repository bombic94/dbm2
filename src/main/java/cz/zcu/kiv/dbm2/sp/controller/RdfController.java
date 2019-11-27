package cz.zcu.kiv.dbm2.sp.controller;

import java.io.IOException;

import cz.zcu.kiv.dbm2.sp.dto.RdfTypesSelectionDto;
import cz.zcu.kiv.dbm2.sp.model.RdfFormat;
import cz.zcu.kiv.dbm2.sp.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;


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

    @PostMapping(value="/confirm", params = "save")
    public String saveConfirm() {
        return "redirect:/download";
    }

    @PostMapping(value="/confirm", params = "cancel")
    public String cancelConfirm() {
        return "redirect:/rename";
    }

    @GetMapping("/download")
    public void getFile(HttpServletResponse response) {
        try {
            // get your file as InputStream
            org.apache.jena.rdf.model.Model renamedModel = rdfService.getRenamedModel();
            RdfFormat rdfFormat = rdfService.getRdfFormat();

            renamedModel.write(response.getOutputStream(), rdfFormat.getLang());
            response.setContentType(rdfFormat.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"renamed." + rdfFormat.getExtension() + "\"");
            response.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException("IOError writing file to output stream");
        }

    }
}
