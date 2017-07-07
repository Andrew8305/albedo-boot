package com.albedo.java.modules.sys.web;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.albedo.java.common.domain.base.DataEntity;
import com.albedo.java.common.security.AuthoritiesConstants;
import com.albedo.java.common.security.SecurityUtil;
import com.albedo.java.common.security.service.InvocationSecurityMetadataSourceService;
import com.albedo.java.modules.sys.domain.Module;
import com.albedo.java.modules.sys.service.ModuleService;
import com.albedo.java.util.JedisUtil;
import com.albedo.java.util.JsonUtil;
import com.albedo.java.util.PublicUtil;
import com.albedo.java.util.StringUtil;
import com.albedo.java.util.base.Reflections;
import com.albedo.java.util.domain.Globals;
import com.albedo.java.util.domain.PageModel;
import com.albedo.java.util.exception.RuntimeMsgException;
import com.albedo.java.vo.sys.query.ModuleTreeQuery;
import com.albedo.java.web.rest.ResultBuilder;
import com.albedo.java.web.rest.base.DataResource;
import com.alibaba.fastjson.JSON;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

/**
 * REST controller for managing Station.
 */
@Controller
@RequestMapping("${albedo.adminPath}/sys/module")
public class ModuleResource extends DataResource<ModuleService, Module> {

    @Resource
    private ModuleService moduleService;

    @ModelAttribute
    public Module get(@RequestParam(required = false) String id) throws Exception {
        String path = request.getRequestURI();
        if (path != null && !path.contains("checkBy") && !path.contains("find") && PublicUtil.isNotEmpty(id)) {
            Module module = moduleService.findOne(id);
            System.out.print(module.getParentIds());
            return module;
        } else {
            return new Module();
        }
    }

    @RequestMapping(value = "findTreeData", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity findTreeData(ModuleTreeQuery moduleTreeQuery) {
        List<Map<String, Object>> rs = moduleService.findTreeData(moduleTreeQuery, SecurityUtil.getModuleList());
        return ResultBuilder.buildOk(rs);
    }


    @RequestMapping(value = "/ico", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String ico() {
        return "modules/sys/moduleIco";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public String list() {
        return "modules/sys/moduleList";
    }

    /**
     * @param pm
     * @return
     */
    @RequestMapping(value = "/page", method = RequestMethod.GET)
    public ResponseEntity getPage(PageModel<Module> pm) {
        moduleService.findPage(pm, SecurityUtil.dataScopeFilter());
        pm.setSortDefaultName(Direction.DESC, DataEntity.F_LASTMODIFIEDDATE);
        JSON rs = JsonUtil.getInstance().toJsonObject(pm);
        return ResultBuilder.buildObject(rs);
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public String form(Module module) {
        if (module == null) {
            throw new RuntimeMsgException(PublicUtil.toAppendStr("查询模块管理失败，原因：无法查找到编号区域"));
        }
        if (StringUtil.isBlank(module.getId())) {
            List<Module> list = moduleService.findAllByParentId(module.getParentId());
            if (list.size() > 0) {
                module.setSort(list.get(list.size() - 1).getSort());
                if (module.getSort() != null) {
                    module.setSort(module.getSort() + 30);
                }
            }
        }
        if (PublicUtil.isNotEmpty(module.getParentId())) {
            module.setParent(moduleService.findOne(module.getParentId()));
        }
        return "modules/sys/moduleForm";
    }

    /**
     * @param module
     * @return
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity save(Module module) {
        log.debug("REST request to save Module : {}", module);
        // Lowercase the module login before comparing with database
        if (PublicUtil.isNotEmpty(module.getPermission()) && !checkByProperty(Reflections.createObj(Module.class, Lists.newArrayList(Module.F_ID, Module.F_PERMISSION),
                module.getId(), module.getPermission()))) {
            throw new RuntimeMsgException("权限已存在");
        }
        moduleService.save(module);
        SecurityUtil.clearUserJedisCache();
        JedisUtil.removeSys(InvocationSecurityMetadataSourceService.RESOURCE_MODULE_DATA_MAP);
        return ResultBuilder.buildOk("保存", module.getName(), "成功");
    }

    /**
     * @param ids
     * @return
     */
    @RequestMapping(value = "/delete/{ids:" + Globals.LOGIN_REGEX
            + "}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity delete(@PathVariable String ids) {
        log.debug("REST request to delete Module: {}", ids);
        moduleService.delete(Lists.newArrayList(ids.split(StringUtil.SPLIT_DEFAULT)));
        SecurityUtil.clearUserJedisCache();
        JedisUtil.removeSys(InvocationSecurityMetadataSourceService.RESOURCE_MODULE_DATA_MAP);
        return ResultBuilder.buildOk("删除成功");
    }

    /**
     * @param ids
     * @return
     */
    @RequestMapping(value = "/lock/{ids:" + Globals.LOGIN_REGEX
            + "}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity lockOrUnLock(@PathVariable String ids) {
        log.debug("REST request to lockOrUnLock User: {}", ids);
        moduleService.lockOrUnLock(Lists.newArrayList(ids.split(StringUtil.SPLIT_DEFAULT)));
        SecurityUtil.clearUserJedisCache();
        JedisUtil.removeSys(InvocationSecurityMetadataSourceService.RESOURCE_MODULE_DATA_MAP);
        return ResultBuilder.buildOk("操作成功");
    }

}
