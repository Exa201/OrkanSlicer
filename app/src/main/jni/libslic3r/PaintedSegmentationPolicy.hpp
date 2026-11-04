#ifndef slic3r_PaintedSegmentationPolicy_hpp_
#define slic3r_PaintedSegmentationPolicy_hpp_

#include <algorithm>
#include <cstddef>

namespace Slic3r {

// Facet state 0 is unpainted; the remaining states correspond to configured filaments.
// Deliberately do not size this from filament_colour: imported profiles may have a short palette.
inline size_t painted_segmentation_state_count(size_t filament_count)
{
    return filament_count + 1;
}

// A segmentation row can be shorter than the configured filament vectors when an imported
// project carries an incomplete color palette. Keep consumers within the row's actual bounds.
inline size_t painted_segmentation_layer_channel_count(size_t filament_count, size_t row_channels)
{
    return std::min(filament_count, row_channels);
}

} // namespace Slic3r

#endif // slic3r_PaintedSegmentationPolicy_hpp_
