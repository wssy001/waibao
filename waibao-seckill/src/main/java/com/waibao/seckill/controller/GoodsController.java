package com.waibao.seckill.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.seckill.entity.Goods;
import com.waibao.seckill.mapper.GoodsMapper;
import com.waibao.seckill.service.cache.HotGoodsDetailCacheService;
import com.waibao.util.enums.ResultEnum;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.seckill.GoodsVO;
import com.waibao.util.vo.user.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GoodsController
 *
 * @author alexpetertyler
 * @since 2022/3/30
 */
@RestController
@RequestMapping("/goods")
@RequiredArgsConstructor
public class GoodsController {
    private final GoodsMapper goodsMapper;
    private final HotGoodsDetailCacheService goodsCacheService;

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<GoodsVO> getUserInfo(
            @RequestParam("goodsId") Long goodsId
    ) {
        Goods goods = goodsCacheService.get(goodsId);
        if (goods == null) return GlobalResult.error("商品ID不存在");
        return GlobalResult.success(BeanUtil.copyProperties(goods, GoodsVO.class));
    }

    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<PageVO<GoodsVO>> getUserPage(
            @RequestBody PageVO<GoodsVO> pageVO
    ) {
        IPage<Goods> goodsPage = new Page<>(pageVO.getIndex(), pageVO.getCount());
        goodsPage = goodsMapper.selectPage(goodsPage, Wrappers.<Goods>lambdaQuery().orderByDesc(Goods::getUpdateTime));

        List<Goods> records = goodsPage.getRecords();
        if (records == null) records = new ArrayList<>();

        List<GoodsVO> goodsVOList = records.parallelStream()
                .map(goods -> BeanUtil.copyProperties(goods, GoodsVO.class))
                .collect(Collectors.toList());

        pageVO.setMaxIndex(goodsPage.getPages());
        pageVO.setList(goodsVOList);
        pageVO.setMaxSize(goodsPage.getTotal());
        return GlobalResult.success(ResultEnum.SUCCESS, pageVO);
    }

    @PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<GoodsVO> addUserInfo(
            @RequestBody GoodsVO goodsVO
    ) {
        Goods goods = BeanUtil.copyProperties(goodsVO, Goods.class);
        goodsMapper.insert(goods);
        goodsCacheService.set(goods);
        goodsVO.setId(goods.getId());
        return GlobalResult.success(ResultEnum.SUCCESS, goodsVO);
    }

}
